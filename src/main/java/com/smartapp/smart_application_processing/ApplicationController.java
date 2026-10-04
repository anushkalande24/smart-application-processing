package com.smartapp.smart_application_processing;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationRepository repository;

    public ApplicationController(ApplicationRepository repository) {
        this.repository = repository;
    }

    @PostMapping(consumes = "multipart/form-data")
    public Application submitApplication(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String category,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MultipartFile document) {

        Application application = new Application();

        application.setName(name);
        application.setEmail(email);
        application.setPhone(phone);

        // Use the category selected by the user
        application.setCategory(category);

        application.setDescription(description);

        // Automatic priority detection
        String descriptionText =
                description == null ? "" : description.toLowerCase();

        if (descriptionText.contains("urgent")
                || descriptionText.contains("emergency")
                || category.equalsIgnoreCase("Government")) {

            application.setPriority("HIGH");

        } else if (category.equalsIgnoreCase("Employment")
                || category.equalsIgnoreCase("Education")) {

            application.setPriority("MEDIUM");

        } else {

            application.setPriority("NORMAL");
        }

        // Check missing fields
        List<String> missing = new ArrayList<>();

        if (name == null || name.isBlank()) {
            missing.add("Name");
        }

        if (email == null || email.isBlank()) {
            missing.add("Email");
        }

        if (phone == null || phone.isBlank()) {
            missing.add("Phone");
        }

        if (category == null || category.isBlank()) {
            missing.add("Category");
        }

        // Document processing
        if (document == null || document.isEmpty()) {

            application.setDocumentName("No document");
            application.setDocumentStatus("MISSING");
            missing.add("Document");

        } else {

            String fileName = document.getOriginalFilename();

            application.setDocumentName(fileName);

            String lowerFileName =
                    fileName == null ? "" : fileName.toLowerCase();

            if (lowerFileName.endsWith(".pdf")
                    || lowerFileName.endsWith(".doc")
                    || lowerFileName.endsWith(".docx")
                    || lowerFileName.endsWith(".jpg")
                    || lowerFileName.endsWith(".jpeg")
                    || lowerFileName.endsWith(".png")) {

                application.setDocumentStatus("PROCESSED");

            } else {

                application.setDocumentStatus("INVALID");
                missing.add("Valid Document");
            }
        }

        // Application status
        if (!missing.isEmpty()) {

            application.setStatus("CORRECTION_REQUIRED");
            application.setMissingFields(String.join(", ", missing));

        } else {

            application.setStatus("PROCESSED");
            application.setMissingFields("None");
        }

        // Save to MySQL
        return repository.save(application);
    }

    // Get all applications
    @GetMapping
    public List<Application> getApplications() {
        return repository.findAll();
    }

    // Update application status
    @PutMapping("/{id}/status")
    public Application updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        Application application = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Application not found"));

        application.setStatus(status);

        return repository.save(application);
    }

    // Delete one application
    @DeleteMapping("/{id}")
    public void deleteApplication(@PathVariable Long id) {
        repository.deleteById(id);
    }
}