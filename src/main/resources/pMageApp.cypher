merge (s:System {name:'Application'})
merge (e1:Event {name: "saved"})
merge (a1:Action {name: "commit-pushed"})
merge (e1)-[:PART_OF]->(s)
merge (a1)-[:IS_A]->(e1)
merge (s1:System {name:"github"})
merge (s2:System {name:"gitlab"})
merge (s1)-[:IS_A]->(s)
merge (s2)-[:IS_A]->(s)
merge (a1)-[:PART_OF]->(s1)
merge (a1)-[:PART_OF]->(s2)
merge (m1:Method {name:'GET'})

// for github
merge (a11:Action {name:"https://api.github.com/repos/{username}/{projectName}/commits"})
merge (p1:PathVariable {name:"username"})
merge (p2:PathVariable {name:"projectName"})
merge (m1)-[:PART_OF]->(a11)
merge (p1)-[:PART_OF]->(a11)
merge (p2)-[:PART_OF]->(a11)
merge (a11)-[:IS_A]->(a1)
merge (a11)-[:PART_OF]->(s1)

// for gitlab
merge (a12:Action {name:"https://gitlab.com/api/v4/projects/{username}%2{projectName}/repository/commits"})
merge (m1)-[:PART_OF]->(a12)
merge (p1)-[:PART_OF]->(a12)
merge (p2)-[:PART_OF]->(a12)
merge (a12)-[:IS_A]->(a1)
merge (a12)-[:PART_OF]->(s2);

// for microsoft word
match (s:System {name:'Application'})
match (e1:Event {name: "saved"})
merge (s3:System {name:"msword"})
merge (s3)-[:IS_A]->(s)
merge (a13:Action {name:"Click Save Icon"})
merge (a13)-[:PART_OF]->(s3)
merge (a13)-[:IS_A]->(e1);