package com.db.hackathon.prompts;

public class PromptLoader {

    public static String loadPrompt(String promptName) {
        // Load the prompt from a file or resource based on the promptName
        // For simplicity, we will return a hardcoded prompt here
        switch (promptName) {
            case "examplePrompt":
                return "This is an example prompt.";
            default:
                return "Prompt not found.";
        }
    }
}
