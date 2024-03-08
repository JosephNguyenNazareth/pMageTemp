package com.pmsconnect.mage;

import com.pmsconnect.mage.utils.ExternalService;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ExternalServiceTest {
    @Test
     public void testPython() {
        File directory = new File("./src/main/python");
//        String envCommand = "mamba activate fouille";
//        String extractCommand = "python3 keyword_extract.py \"beautiful girl is a flower | never underestimate a cat\"" ;
//        String extractCommand = "mamba run -n fouille python3 keyword_extract.py";
        // creating list of commands
        List<String> commands = new ArrayList<String>();
        commands.add("/home/nguyenminhkhoi/mambaforge/envs/fouille/bin/python");
        commands.add("keyword_extract.py");
        commands.add("\"beautiful girl is a flower | never underestimate a cat\"");
        try {
            String keywords = ExternalService.runCommand(directory, commands);
            System.out.println(keywords);
            assertEquals(keywords, "girl is flower | never underestimate cat");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
