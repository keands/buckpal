package com.buckpal.controller;

import com.buckpal.dto.csv.*;
import com.buckpal.entity.CsvMappingTemplate;
import com.buckpal.entity.User;
import com.buckpal.service.CsvImportWizardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/csv-import")
@CrossOrigin(origins = "*")
@Tag(name = "CSV Import", description = "API d'importation de transactions via fichiers CSV")
@SecurityRequirement(name = "bearerAuth")
public class CsvImportController {
    
    @Autowired
    private CsvImportWizardService csvImportWizardService;
    
    /**
     * Step 1: Upload CSV file and get preview
     */
    @PostMapping("/upload")
    @Operation(summary = "Upload fichier CSV", description = "Étape 1: Télécharge un fichier CSV et retourne un aperçu des données")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Fichier uploadé avec succès",
                content = @Content(schema = @Schema(implementation = CsvUploadResponse.class))),
        @ApiResponse(responseCode = "400", description = "Fichier invalide ou vide"),
        @ApiResponse(responseCode = "500", description = "Erreur serveur lors de l'upload")
    })
    public ResponseEntity<?> uploadCsv(
            @Parameter(description = "Fichier CSV à importer", required = true)
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Le fichier CSV est vide");
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
                return ResponseEntity.badRequest().body("Le fichier doit être au format CSV");
            }
            
            CsvUploadResponse response = csvImportWizardService.uploadCsv(file);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'upload du fichier: " + e.getMessage());
        }
    }
    
    /**
     * Step 2: Process column mapping and get validation preview
     */
    @PostMapping("/mapping")
    public ResponseEntity<?> processMappingAndPreview(@RequestBody CsvColumnMappingRequest request) {
        try {
            // Validate required fields
            if (request.getSessionId() == null || request.getAccountId() == null) {
                return ResponseEntity.badRequest().body("Session ID et Account ID sont requis");
            }
            
            if (request.getDateColumnIndex() == null) {
                return ResponseEntity.badRequest().body("La colonne de date est requise");
            }
            
            if (request.getAmountColumnIndex() == null && 
                (request.getDebitColumnIndex() == null || request.getCreditColumnIndex() == null)) {
                return ResponseEntity.badRequest()
                        .body("Une colonne de montant ou des colonnes débit/crédit sont requises");
            }
            
            CsvPreviewResponse response = csvImportWizardService.processMappingAndPreview(request);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du traitement du mapping: " + e.getMessage());
        }
    }
    
    /**
     * Step 3: Validate and finalize import
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateAndImport(@RequestBody CsvValidationRequest request) {
        try {
            if (request.getSessionId() == null) {
                return ResponseEntity.badRequest().body("Session ID est requis");
            }
            
            CsvImportResult result = csvImportWizardService.finalizeImport(request);
            return ResponseEntity.ok(result);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'importation finale: " + e.getMessage());
        }
    }
    
    /**
     * Get saved mapping templates for the current user
     */
    @GetMapping("/templates")
    public ResponseEntity<?> getMappingTemplates(Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            List<CsvMappingTemplate> templates = csvImportWizardService.getMappingTemplates(userId);
            return ResponseEntity.ok(templates);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération des templates: " + e.getMessage());
        }
    }
    
    /**
     * Apply a saved mapping template
     */
    @PostMapping("/templates/{bankName}/apply")
    public ResponseEntity<?> applySavedMapping(
            @PathVariable String bankName,
            @RequestParam String sessionId,
            Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            CsvColumnMappingRequest mapping = csvImportWizardService
                    .applySavedMapping(sessionId, bankName, userId);
            return ResponseEntity.ok(mapping);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'application du template: " + e.getMessage());
        }
    }
    
    /**
     * Get CSV template for download
     */
    @GetMapping("/template")
    public ResponseEntity<String> getCsvTemplate() {
        String template = "Date,Description,Amount,Category\n" +
                         "01/12/2023,Achat supermarché,-45.67,Alimentation\n" +
                         "02/12/2023,Salaire,2500.00,Revenus\n" +
                         "03/12/2023,Facture électricité,-89.45,Utilities";
        
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=template.csv")
                .body(template);
    }
    
    // Helper methods
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }
        
        User user = (User) authentication.getPrincipal();
        return user.getId();
    }
}