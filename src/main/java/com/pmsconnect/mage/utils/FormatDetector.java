package com.pmsconnect.mage.utils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.SAXException;
import org.yaml.snakeyaml.Yaml;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class FormatDetector {
    public static boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (Exception e1) {
            try {
                new JSONArray(test);
            } catch (Exception e2) {
                return false;
            }
        }
        System.out.println("JSON format detected.");
        return true;
    }

    public static boolean isXMLValid(String test) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(new ByteArrayInputStream(test.getBytes()));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            return false;
        }
        System.out.println("XML format detected.");
        return true;
    }

    public static boolean isYAMLValid(String test) {
        try {
            Yaml yaml = new Yaml();
            yaml.load(test);
        } catch (Exception e) {
            return false;
        }
        System.out.println("YAML format detected.");
        return true;
    }

    public static String detectFormat(String text) {
        if (isJSONValid(text)) {
            return "JSON";
        } else if (isXMLValid(text)) {
            return "XML";
        } else if (isYAMLValid(text)) {
            return "YAML";
        } else {
            return "Unknown format";
        }
    }
}
