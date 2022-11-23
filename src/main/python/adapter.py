import json
from unidecode import unidecode

import requests
from requests.auth import HTTPBasicAuth

from datetime import datetime
import pytz

import os
from pymongo import MongoClient

class Adapter:
    def __init__(self):
        self.repo_list = []
        self.read_repo_config()
        self.current_repo_info = {}
    

    def load_repo(self, repo_link):
        self.repo_list.append(repo_link)


    def load_repo_list(self, repo_list):
        self.repo_list += repo_list


    def load_repo_list_from_file(self, path):
        file1 = open(path, 'r')
        lines = file1.readlines()
        
        count = 0
        urls = []
        for line in lines:
            count += 1
            urls.append(line.strip())

        print(f"We found {count} repo links.")

        self.load_repo_list(urls)


    def read_repo_config(self):
        default_file = "repo_config.json"
        if os.path.isfile(default_file):
            config_file = open(default_file)
            self.config = json.load(config_file)
            config_file.close()
        else:
            self.config = []


    def append_repo_config(self):
        self.config.append(self.current_repo_info)
        config_file = open("repo_config.json","w")
        json.dump(self.config, config_file, indent=4)
        config_file.close()


    def update_repo_config(self):
        update_index = -1
        for i in range(len(self.config)):
            if self.config[i]["origin"] == self.current_repo_info["origin"]:
                update_index = i
                break

        self.config[update_index].update(self.current_repo_info)
        config_file = open("repo_config.json","w")
        json.dump(self.config, config_file, indent=4)
        config_file.close()
        

    def init_repo_info(self):
        self.current_repo_info["origin"] = input("Origin: ")
    
    def get_repo_info(self):
        tmp = {}
        tmp["api_prefix"] = input("API prefix: ")
        tmp["api_postfix_commit"] = input("API postfix for commits retrievement: ")
        tmp["api_postfix_diff"] = input("API postfix for commits diff: ")
        self.current_repo_info["api_info"] = tmp

        tmp = {}
        tmp["user"] = input("Authenticated user: ")
        tmp["token"] = input("Personal Access Token: ")
        self.current_repo_info["user_info"] = tmp    

    
    def update_repo_info(self):
        tmp = {}
        print("Please use | to refer the hierachical level of the field")
        tmp["id"] = input("id field in return message: ")
        tmp["time"] = input("time field in return message: ")
        tmp["title"] = input("title field in return message: ")
        tmp["committer"] = input("committer name field in return message: ")
    
        self.current_repo_info["message_info"] = tmp


    def update_repo_diff_info(self):
        tmp = {}
        print("Please use | to refer the hierachical level of the field")
        tmp["file_change"] = input("list of file changed field in return message: ")
        tmp["path"] = input("path field in return message: ")
        nb_status_field = int(input("how many fields concerning the status of the file: "))
        if nb_status_field == 1:
            tmp["status"] = input("status field in return message: ")
        elif nb_status_field > 1:
            tmp["status"] = []
            for i in range(nb_status_field):
                tmp["status"].append(input("status field " + str(i + 1) + "in return message: "))
    
        self.current_repo_info["diff_info"] = tmp


    def get_repo_origin(self, repo_link):
        for repo_config in self.config:
            if repo_config["origin"] in repo_link:
                return repo_config
        return {}


    def build_api_link_commit(self, repo_link, config):
        config_info = config["api_info"]
        if config["origin"] == "github.com":
            return repo_link.replace(config["origin"], config_info["api_prefix"]) + "/" + config_info["api_postfix_commit"]
        elif config["origin"] == "gitlab.com":
            url_path = repo_link[repo_link.find(config["origin"]) + len(config["origin"]) + 1:].replace("/", "%2F")
            return "http://" + config_info["api_prefix"] + "/" + url_path + "/" + config_info["api_postfix_commit"]


    def build_api_link_diff(self, repo_link, config, commit_id):
        config_info = config["api_info"]
        tmp = repo_link.replace(config["origin"], config_info["api_prefix"]) + "/" + config_info["api_postfix_commit"] + "/" + commit_id + "/" + config_info["api_postfix_diff"]
        if tmp[-1] == "/":
            tmp = tmp[:-1]

        return tmp


    def call_api(self, api_link, config):
        config_info = config["user_info"]
        try:
            resp = requests.get(api_link, auth = HTTPBasicAuth(config_info["user"], config_info["token"]))
            good_form = unidecode(resp.text)
            origins = json.loads(good_form)
        except (json.decoder.JSONDecodeError, json.JSONDecodeError):
            return []

        if type(origins) != list:
            return []
            
        return origins


    def call_api_diff(self, api_link, config):
        config_info = config["user_info"]
        try:
            resp = requests.get(api_link, auth = HTTPBasicAuth(config_info["user"], config_info["token"]))
            good_form = unidecode(resp.text)
            origins = json.loads(good_form)
        except (json.decoder.JSONDecodeError, json.JSONDecodeError):
            return []
            
        return origins


    def traverse_message_level(self, message, path):
        # path has the format of "level1|level2|level3"
        tmp = message
        levels = path.split("|")

        for keyword in levels:
            if keyword in tmp.keys():
                tmp = tmp[keyword]

        return tmp


    def extract_info_commit(self, repo_link, origin_message, repo_config):
        message_info_config = repo_config["message_info"]
        local_tz = pytz.timezone("Europe/Paris")
        extract_commits = []

        for commit in origin_message:
            extract_info = {}
            extract_info["id"] = self.traverse_message_level(commit, message_info_config["id"])

            tmp_time = self.traverse_message_level(commit, message_info_config["time"])
            tmp_time = datetime.strptime(tmp_time, repo_config["api_info"]["time_format"])
            tmp_time = tmp_time.astimezone(local_tz)
            extract_info["created_at"] = tmp_time.isoformat(sep='T', timespec='milliseconds')
            
            extract_info["title"] = self.traverse_message_level(commit, message_info_config["title"]).strip().replace("\'", "\"")
            extract_info["committer_name"] = self.traverse_message_level(commit, message_info_config["committer"]).strip()
            extract_info["project_id"] = repo_link
            extract_info["origin"] = repo_config["origin"]

            extract_commits.append(extract_info)

        return extract_commits


    def extract_info_diff(self, commit_id, origin_message, repo_config):
        diff_info_config = repo_config["diff_info"]

        extract_diff = {}

        extract_diff["id"] = commit_id
        extract_diff["diff"] = []

        file_changes = self.traverse_message_level(origin_message, diff_info_config["file_change"])

        for file_change in file_changes:
            extract_info = {}
            extract_info["path"] = self.traverse_message_level(file_change, diff_info_config["path"])

            if type(diff_info_config["status"] == list):
                final_changes = ""
                for status in diff_info_config["status"]:
                    if self.traverse_message_level(file_change, status):
                        final_changes += status + ","
                final_changes = final_changes[:-1] # remove the last comma
                extract_info["status"] = final_changes
            else:
                extract_info["status"] = self.traverse_message_level(file_change, diff_info_config["status"])

            extract_diff["diff"].append(extract_info)

        return extract_diff


    def get_commit_log(self):
        extract_commits = []
        for repo_link in self.repo_list:
            repo_detected = self.get_repo_origin(repo_link)
            if "origin" not in self.current_repo_info.keys():
                self.current_repo_info["origin"] = repo_detected["origin"]

            if "api_info" not in repo_detected.keys():
                self.get_repo_info()
                repo_detected.update(self.current_repo_info)

            api_link = self.build_api_link_commit(repo_link, repo_detected)
            original_commit = self.call_api(api_link, repo_detected)

            if "message_info" not in repo_detected.keys():
                # print(original_commit)
                self.update_repo_info()
                repo_detected.update(self.current_repo_info)
                self.update_repo_config()

            extract_commits += self.extract_info_commit(repo_link, original_commit, repo_detected)

        return extract_commits


    def get_commit_diff(self, commit_list):
        extract_diffs = []
        for commit in commit_list:
            repo_detected = self.get_repo_origin(commit["project_id"])
            api_link = self.build_api_link_diff(commit["project_id"], repo_detected, commit["id"])
            original_diff = self.call_api_diff(api_link, repo_detected)

            if "diff_info" not in repo_detected.keys():
                # print(original_diff)
                self.update_repo_diff_info()
                repo_detected.update(self.current_repo_info)
                self.update_repo_config()

            extract_diffs += self.extract_info_diff(commit["id"], original_diff, repo_detected)

        return extract_diffs


    def tag_project(self, repo_list):
        list_project = []
        for proj in repo_list:
            proj_info = {}
            proj_info["id"] = proj
            result = self.get_repo_origin(proj)
            if result == {}:
                self.init_repo_info()
                self.append_repo_config()
                result = self.current_repo_info

            proj_info["source"] = result["origin"]
            list_project.append(proj_info)

        return list_project


    def save_data(self, project_name, project_result, commit_result, diff_result):
        try:
            client = MongoClient('localhost', 27017)
        except:
            print("Error")

        db = client[project_name]

        collection_projects = db['projects']
        collection_commits = db['commits']
        collection_diffs = db['diffs']

        collection_projects.insert_many(project_result)
        collection_commits.insert_many(commit_result)
        collection_diffs.insert_many(diff_result)

        client.close()


    def clear_data(project_name):
        try:
            client = MongoClient('localhost', 27017)
        except:
            print("Error")

        db = client[project_name]

        collection_projects = db['projects']
        collection_commits = db['commits']
        collection_diffs = db['diffs']

        collection_projects.drop()
        collection_commits.drop()
        collection_diffs.drop()

        client.close()


    def extract_info_repo_lists(self, path):
        self.load_repo_list_from_file(path)
        projects = self.tag_project(self.repo_list)
        commits = self.get_commit_log()
        diffs = self.get_commit_diff(commits)

        project = "adapter"
        # self.save_data(project, projects, commits, diffs)


    def get_latest_commit_log(self, take_all):
        extract_commits = []
        for repo_link in self.repo_list:
            repo_detected = self.get_repo_origin(repo_link)
            if "origin" not in self.current_repo_info.keys():
                self.current_repo_info["origin"] = repo_detected["origin"]

            if "api_info" not in repo_detected.keys():
                self.get_repo_info()
                repo_detected.update(self.current_repo_info)

            api_link = self.build_api_link_commit(repo_link, repo_detected)
            original_commit = self.call_api(api_link, repo_detected) # to get the latest commit on server

            if "message_info" not in repo_detected.keys():
                # print(original_commit)
                self.update_repo_info()
                repo_detected.update(self.current_repo_info)
                self.update_repo_config()
            
            if not take_all:
                original_commit = [original_commit[0]]
            extract_commits += self.extract_info_commit(repo_link, original_commit, repo_detected)

        return extract_commits

    
    def get_latest_commit(self, take_all):
        latest_commits = self.get_latest_commit_log(take_all)
        # latest_diff = self.get_commit_diff(latest_commits)
        if not take_all:
            print(latest_commits[0])
        else:
            print(latest_commits)
