package com.buckpal.service;

import com.buckpal.entity.MerchantPattern;
import com.buckpal.repository.MerchantPatternRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class MerchantPatternMigrationService {
    
    private final MerchantPatternRepository merchantPatternRepository;
    
    @Autowired
    public MerchantPatternMigrationService(MerchantPatternRepository merchantPatternRepository) {
        this.merchantPatternRepository = merchantPatternRepository;
    }
    
    public void migrateFrenchPatterns() {
        Map<String, Integer> categoryPatterns = createFrenchCategoryPatterns();
        
        for (Map.Entry<String, Integer> entry : categoryPatterns.entrySet()) {
            String[] parts = entry.getKey().split(":");
            String categoryName = parts[0];
            String pattern = parts[1];
            Integer specificity = entry.getValue();
            
            // Check if pattern already exists
            if (merchantPatternRepository.findByPatternAndCategoryName(pattern, categoryName).isEmpty()) {
                MerchantPattern merchantPattern = new MerchantPattern(pattern, categoryName, specificity);
                merchantPatternRepository.save(merchantPattern);
            }
        }
    }
    
    private Map<String, Integer> createFrenchCategoryPatterns() {
        Map<String, Integer> patterns = new HashMap<>();
        
        // Alimentation - Grocery patterns
        patterns.put("Alimentation:CARREFOUR", 8);
        patterns.put("Alimentation:LECLERC", 8);
        patterns.put("Alimentation:AUCHAN", 8);
        patterns.put("Alimentation:INTERMARCHE", 8);
        patterns.put("Alimentation:CASINO", 7);
        patterns.put("Alimentation:FRANPRIX", 8);
        patterns.put("Alimentation:MONOPRIX", 8);
        patterns.put("Alimentation:PICARD", 7);
        patterns.put("Alimentation:BIOCOOP", 8);
        patterns.put("Alimentation:NATURALIA", 9);
        patterns.put("Alimentation:GRAND FRAIS", 10);
        patterns.put("Alimentation:LIDL", 5);
        patterns.put("Alimentation:ALDI", 5);
        patterns.put("Alimentation:NETTO", 6);
        patterns.put("Alimentation:LEADER PRICE", 12);
        patterns.put("Alimentation:U EXPRESS", 9);
        patterns.put("Alimentation:SUPERMARCHE", 11);
        patterns.put("Alimentation:EPICERIE", 8);
        patterns.put("Alimentation:BOULANGERIE", 11);
        patterns.put("Alimentation:BOUCHERIE", 9);
        patterns.put("Alimentation:CHARCUTERIE", 11);
        patterns.put("Alimentation:POISSONNERIE", 12);
        patterns.put("Alimentation:FROMAGERIE", 10);
        patterns.put("Alimentation:MARCHE", 6);
        
        // Transport patterns
        patterns.put("Transport:SNCF", 4);
        patterns.put("Transport:RATP", 4);
        patterns.put("Transport:NAVIGO", 6);
        patterns.put("Transport:VELIB", 5);
        patterns.put("Transport:AUTOLIB", 7);
        patterns.put("Transport:CITIZ", 5);
        patterns.put("Transport:UBER", 4);
        patterns.put("Transport:BLABLACAR", 9);
        patterns.put("Transport:OUIGO", 5);
        patterns.put("Transport:TER", 3);
        patterns.put("Transport:TGV", 3);
        patterns.put("Transport:TOTAL", 5);
        patterns.put("Transport:BP", 2);
        patterns.put("Transport:ESSO", 4);
        patterns.put("Transport:SHELL", 5);
        patterns.put("Transport:AGIP", 4);
        patterns.put("Transport:STATION SERVICE", 15);
        patterns.put("Transport:ESSENCE", 7);
        patterns.put("Transport:GASOIL", 6);
        patterns.put("Transport:CARBURANT", 9);
        patterns.put("Transport:PARKIMETER", 10);
        patterns.put("Transport:PARKING", 7);
        patterns.put("Transport:PEAGE", 5);
        patterns.put("Transport:AUTOROUTE", 9);
        patterns.put("Transport:VINCI PARK", 10);
        patterns.put("Transport:EFFIA", 6);
        patterns.put("Transport:INDIGO", 6);
        
        // Factures - Bills patterns
        patterns.put("Factures:EDF", 3);
        patterns.put("Factures:GDF", 3);
        patterns.put("Factures:ENGIE", 5);
        patterns.put("Factures:VEOLIA", 6);
        patterns.put("Factures:SUEZ", 4);
        patterns.put("Factures:ORANGE", 6);
        patterns.put("Factures:SFR", 3);
        patterns.put("Factures:BOUYGUES", 8);
        patterns.put("Factures:FREE", 4);
        patterns.put("Factures:NUMERICABLE", 11);
        patterns.put("Factures:BBOX", 4);
        patterns.put("Factures:LIVEBOX", 7);
        patterns.put("Factures:FREEBOX", 7);
        patterns.put("Factures:ELECTRICITE", 11);
        patterns.put("Factures:GAZ", 3);
        patterns.put("Factures:EAU", 3);
        patterns.put("Factures:TELEPHONIE", 10);
        patterns.put("Factures:INTERNET", 8);
        patterns.put("Factures:ASSURANCE", 9);
        patterns.put("Factures:MUTUELLE", 8);
        
        // Santé - Health patterns
        patterns.put("Santé:PHARMACIE", 9);
        patterns.put("Santé:DOCTEUR", 7);
        patterns.put("Santé:MEDECIN", 7);
        patterns.put("Santé:DENTISTE", 8);
        patterns.put("Santé:LABORATOIRE", 11);
        patterns.put("Santé:RADIOLOGUE", 10);
        patterns.put("Santé:OPHTALMO", 9);
        patterns.put("Santé:CARDIOLOGUE", 11);
        patterns.put("Santé:DERMATOLOGUE", 12);
        patterns.put("Santé:GYNECO", 6);
        patterns.put("Santé:KINESITHERAPEUTE", 16);
        patterns.put("Santé:OSTEOPATHE", 10);
        patterns.put("Santé:HOPITAL", 7);
        patterns.put("Santé:CLINIQUE", 8);
        patterns.put("Santé:CPAM", 4);
        patterns.put("Santé:SECU", 4);
        
        // Banque - Banking patterns
        patterns.put("Banque:BNP", 3);
        patterns.put("Banque:PARIBAS", 7);
        patterns.put("Banque:SOCIETE GENERALE", 16);
        patterns.put("Banque:CREDIT AGRICOLE", 14);
        patterns.put("Banque:LCL", 3);
        patterns.put("Banque:CIC", 3);
        patterns.put("Banque:BANQUE POPULAIRE", 15);
        patterns.put("Banque:CAISSE D'EPARGNE", 15);
        patterns.put("Banque:CREDIT MUTUEL", 12);
        patterns.put("Banque:BOURSORAMA", 10);
        patterns.put("Banque:ING", 3);
        patterns.put("Banque:HELLO BANK", 10);
        patterns.put("Banque:FORTUNEO", 8);
        patterns.put("Banque:N26", 3);
        patterns.put("Banque:REVOLUT", 7);
        patterns.put("Banque:VIREMENT", 8);
        patterns.put("Banque:PRELEVEMENT", 11);
        patterns.put("Banque:FRAIS", 5);
        patterns.put("Banque:COMMISSION", 10);
        patterns.put("Banque:COTISATION", 10);
        
        // Divertissement - Entertainment patterns
        patterns.put("Divertissement:NETFLIX", 7);
        patterns.put("Divertissement:DISNEY+", 7);
        patterns.put("Divertissement:PRIME VIDEO", 11);
        patterns.put("Divertissement:SPOTIFY", 7);
        patterns.put("Divertissement:DEEZER", 6);
        patterns.put("Divertissement:CINEMA", 6);
        patterns.put("Divertissement:GAUMONT", 7);
        patterns.put("Divertissement:UGC", 3);
        patterns.put("Divertissement:PATHE", 5);
        patterns.put("Divertissement:FNAC", 4);
        patterns.put("Divertissement:CULTURA", 7);
        patterns.put("Divertissement:MICROMANIA", 10);
        patterns.put("Divertissement:STEAM", 5);
        patterns.put("Divertissement:PLAYSTATION", 11);
        patterns.put("Divertissement:XBOX", 4);
        patterns.put("Divertissement:NINTENDO", 8);
        patterns.put("Divertissement:MATCH", 5);
        patterns.put("Divertissement:CONCERT", 7);
        patterns.put("Divertissement:SPECTACLE", 9);
        patterns.put("Divertissement:THEATRE", 7);
        
        // Restaurant patterns
        patterns.put("Restaurant:MCDONALD", 8);
        patterns.put("Restaurant:BURGER KING", 11);
        patterns.put("Restaurant:KFC", 3);
        patterns.put("Restaurant:QUICK", 5);
        patterns.put("Restaurant:SUBWAY", 6);
        patterns.put("Restaurant:DOMINOS", 7);
        patterns.put("Restaurant:PIZZA HUT", 9);
        patterns.put("Restaurant:DELIVEROO", 10);
        patterns.put("Restaurant:UBER EATS", 9);
        patterns.put("Restaurant:JUST EAT", 8);
        patterns.put("Restaurant:RESTAURANT", 10);
        patterns.put("Restaurant:BRASSERIE", 9);
        patterns.put("Restaurant:BISTROT", 7);
        patterns.put("Restaurant:CAFE", 4);
        patterns.put("Restaurant:BAR", 3);
        patterns.put("Restaurant:PIZZERIA", 8);
        patterns.put("Restaurant:KEBAB", 5);
        patterns.put("Restaurant:SUSHI", 5);
        patterns.put("Restaurant:CHINOIS", 7);
        patterns.put("Restaurant:JAPONAIS", 8);
        patterns.put("Restaurant:ITALIEN", 7);
        patterns.put("Restaurant:INDIEN", 6);
        patterns.put("Restaurant:THAI", 4);
        
        // Vêtements - Clothing patterns
        patterns.put("Vêtements:ZARA", 4);
        patterns.put("Vêtements:H&M", 3);
        patterns.put("Vêtements:UNIQLO", 6);
        patterns.put("Vêtements:C&A", 3);
        patterns.put("Vêtements:PRIMARK", 7);
        patterns.put("Vêtements:KIABI", 5);
        patterns.put("Vêtements:DECATHLON", 9);
        patterns.put("Vêtements:INTERSPORT", 10);
        patterns.put("Vêtements:GO SPORT", 8);
        patterns.put("Vêtements:NIKE", 4);
        patterns.put("Vêtements:ADIDAS", 6);
        patterns.put("Vêtements:LACOSTE", 7);
        patterns.put("Vêtements:CELIO", 5);
        patterns.put("Vêtements:JULES", 5);
        patterns.put("Vêtements:CAMAIEU", 7);
        patterns.put("Vêtements:PROMOD", 6);
        patterns.put("Vêtements:ETAM", 4);
        patterns.put("Vêtements:GALERIES LAFAYETTE", 18);
        patterns.put("Vêtements:PRINTEMPS", 9);
        patterns.put("Vêtements:BHV", 3);
        
        return patterns;
    }
}