package com.buckpal.service;

import com.buckpal.entity.Budget;
import com.buckpal.entity.BudgetCategory;
import com.buckpal.entity.Category;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.User;
import com.buckpal.repository.BudgetCategoryRepository;
import com.buckpal.repository.BudgetRepository;
import com.buckpal.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class EnhancedTransactionAssignmentService {
    
    private final TransactionRepository transactionRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetService budgetService;
    
    // Enhanced patterns for merchant/description matching (French + International)
    private static final Map<String, Set<Pattern>> CATEGORY_PATTERNS = createCategoryPatterns();
    
    // Amount-based patterns (for typical spending ranges in EUR - French market)
    private static final Map<String, AmountRange> CATEGORY_AMOUNT_RANGES = createAmountRanges();
    
    @Autowired
    public EnhancedTransactionAssignmentService(
            TransactionRepository transactionRepository,
            BudgetCategoryRepository budgetCategoryRepository,
            BudgetRepository budgetRepository,
            BudgetService budgetService) {
        this.transactionRepository = transactionRepository;
        this.budgetCategoryRepository = budgetCategoryRepository;
        this.budgetRepository = budgetRepository;
        this.budgetService = budgetService;
    }
    
    /**
     * Enhanced auto-assignment using multiple strategies
     */
    public AssignmentResult autoAssignTransactions(User user, Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
            .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        List<Transaction> monthlyUnassignedTransactions = getUnassignedTransactionsForMonth(
            user, budget.getBudgetMonth(), budget.getBudgetYear());
        
        AssignmentResult result = new AssignmentResult();
        
        for (Transaction transaction : monthlyUnassignedTransactions) {
            AssignmentStrategy bestStrategy = findBestCategoryMatch(transaction, budgetId);
            
            if (bestStrategy.isSuccess()) {
                BudgetCategory budgetCategory = bestStrategy.getBudgetCategory();
                transaction.setBudgetCategory(budgetCategory);
                transaction.setAssignmentStatus(Transaction.AssignmentStatus.AUTO_ASSIGNED);
                transactionRepository.save(transaction);
                
                budgetService.updateBudgetAfterTransactionAssignment(budgetCategory.getId());
                
                result.addAssigned(transaction, bestStrategy.getStrategy(), bestStrategy.getConfidence());
            } else {
                transaction.setAssignmentStatus(Transaction.AssignmentStatus.NEEDS_REVIEW);
                transactionRepository.save(transaction);
                result.addNeedsReview(transaction);
            }
        }
        
        return result;
    }
    
    /**
     * Find the best category match using multiple strategies
     */
    private AssignmentStrategy findBestCategoryMatch(Transaction transaction, Long budgetId) {
        List<AssignmentStrategy> strategies = new ArrayList<>();
        
        // Strategy 1: Existing category mapping (highest priority)
        AssignmentStrategy categoryStrategy = tryExistingCategoryMapping(transaction, budgetId);
        if (categoryStrategy.isSuccess()) {
            strategies.add(categoryStrategy);
        }
        
        // Strategy 2: Historical learning from user's past assignments
        AssignmentStrategy historicalStrategy = tryHistoricalLearning(transaction, budgetId);
        if (historicalStrategy.isSuccess()) {
            strategies.add(historicalStrategy);
        }
        
        // Strategy 3: Merchant/description pattern matching
        AssignmentStrategy patternStrategy = tryPatternMatching(transaction, budgetId);
        if (patternStrategy.isSuccess()) {
            strategies.add(patternStrategy);
        }
        
        // Strategy 4: Amount-based inference
        AssignmentStrategy amountStrategy = tryAmountBasedInference(transaction, budgetId);
        if (amountStrategy.isSuccess()) {
            strategies.add(amountStrategy);
        }
        
        // Strategy 5: Similarity to existing transactions
        AssignmentStrategy similarityStrategy = trySimilarityMatching(transaction, budgetId);
        if (similarityStrategy.isSuccess()) {
            strategies.add(similarityStrategy);
        }
        
        // Return the strategy with the highest confidence
        return strategies.stream()
            .max(Comparator.comparing(AssignmentStrategy::getConfidence))
            .orElse(new AssignmentStrategy("none", null, 0.0));
    }
    
    /**
     * Strategy 1: Use existing category mapping (current implementation)
     */
    private AssignmentStrategy tryExistingCategoryMapping(Transaction transaction, Long budgetId) {
        if (transaction.getCategory() == null) {
            return new AssignmentStrategy("existing_category", null, 0.0);
        }
        
        String categoryKey = transaction.getCategory().getName();
        String budgetCategoryKey = mapTransactionCategoryToBudgetCategory(categoryKey);
        
        if (budgetCategoryKey != null) {
            Optional<BudgetCategory> budgetCategory = budgetCategoryRepository
                .findByBudgetIdAndName(budgetId, budgetCategoryKey);
            
            if (budgetCategory.isPresent()) {
                return new AssignmentStrategy("existing_category", budgetCategory.get(), 0.95);
            }
        }
        
        return new AssignmentStrategy("existing_category", null, 0.0);
    }
    
    /**
     * Strategy 2: Learn from user's historical assignments
     */
    private AssignmentStrategy tryHistoricalLearning(Transaction transaction, Long budgetId) {
        // Find similar transactions that were already assigned by this user
        String description = transaction.getDescription();
        String merchantName = transaction.getMerchantName();
        
        if (description == null && merchantName == null) {
            return new AssignmentStrategy("historical", null, 0.0);
        }
        
        // Get user's previously assigned transactions
        List<Transaction> userTransactions = transactionRepository.findByUser(transaction.getAccount().getUser());
        
        Map<BudgetCategory, Integer> categoryVotes = new HashMap<>();
        
        for (Transaction pastTransaction : userTransactions) {
            if (pastTransaction.getBudgetCategory() != null && 
                pastTransaction.getAssignmentStatus() != Transaction.AssignmentStatus.UNASSIGNED) {
                
                double similarity = calculateTransactionSimilarity(transaction, pastTransaction);
                if (similarity > 0.7) { // High similarity threshold
                    BudgetCategory category = pastTransaction.getBudgetCategory();
                    
                    // Check if this category exists in current budget
                    Optional<BudgetCategory> currentBudgetCategory = budgetCategoryRepository
                        .findByBudgetIdAndName(budgetId, category.getName());
                    
                    if (currentBudgetCategory.isPresent()) {
                        categoryVotes.merge(currentBudgetCategory.get(), 
                            (int)(similarity * 10), Integer::sum);
                    }
                }
            }
        }
        
        if (!categoryVotes.isEmpty()) {
            BudgetCategory mostVoted = Collections.max(categoryVotes.entrySet(), 
                Map.Entry.comparingByValue()).getKey();
            double confidence = Math.min(0.9, categoryVotes.get(mostVoted) / 30.0); // Max confidence 0.9
            return new AssignmentStrategy("historical", mostVoted, confidence);
        }
        
        return new AssignmentStrategy("historical", null, 0.0);
    }
    
    /**
     * Strategy 3: Pattern matching on merchant/description
     */
    private AssignmentStrategy tryPatternMatching(Transaction transaction, Long budgetId) {
        String description = transaction.getDescription();
        String merchantName = transaction.getMerchantName();
        
        if (description == null && merchantName == null) {
            return new AssignmentStrategy("pattern", null, 0.0);
        }
        
        String textToMatch = (description != null ? description + " " : "") + 
                           (merchantName != null ? merchantName : "");
        
        for (Map.Entry<String, Set<Pattern>> entry : CATEGORY_PATTERNS.entrySet()) {
            String categoryName = entry.getKey();
            Set<Pattern> patterns = entry.getValue();
            
            for (Pattern pattern : patterns) {
                if (pattern.matcher(textToMatch).matches()) {
                    Optional<BudgetCategory> budgetCategory = budgetCategoryRepository
                        .findByBudgetIdAndName(budgetId, categoryName);
                    
                    if (budgetCategory.isPresent()) {
                        return new AssignmentStrategy("pattern", budgetCategory.get(), 0.8);
                    }
                }
            }
        }
        
        return new AssignmentStrategy("pattern", null, 0.0);
    }
    
    /**
     * Strategy 4: Amount-based inference
     */
    private AssignmentStrategy tryAmountBasedInference(Transaction transaction, Long budgetId) {
        if (transaction.getTransactionType() != Transaction.TransactionType.EXPENSE) {
            return new AssignmentStrategy("amount", null, 0.0);
        }
        
        double amount = Math.abs(transaction.getAmount().doubleValue());
        
        for (Map.Entry<String, AmountRange> entry : CATEGORY_AMOUNT_RANGES.entrySet()) {
            String categoryName = entry.getKey();
            AmountRange range = entry.getValue();
            
            if (range.contains(amount)) {
                Optional<BudgetCategory> budgetCategory = budgetCategoryRepository
                    .findByBudgetIdAndName(budgetId, categoryName);
                
                if (budgetCategory.isPresent()) {
                    // Lower confidence for amount-only matching
                    double confidence = 0.4 * range.getConfidenceForAmount(amount);
                    return new AssignmentStrategy("amount", budgetCategory.get(), confidence);
                }
            }
        }
        
        return new AssignmentStrategy("amount", null, 0.0);
    }
    
    /**
     * Strategy 5: Similarity to other transactions in the same month
     */
    private AssignmentStrategy trySimilarityMatching(Transaction transaction, Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
            .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        // Find already assigned transactions in the same month
        List<Transaction> assignedTransactions = transactionRepository
            .findByUserAndAssignmentStatus(transaction.getAccount().getUser(), 
                Transaction.AssignmentStatus.AUTO_ASSIGNED)
            .stream()
            .filter(t -> t.getTransactionDate().getMonthValue() == budget.getBudgetMonth() &&
                        t.getTransactionDate().getYear() == budget.getBudgetYear() &&
                        t.getBudgetCategory() != null)
            .collect(Collectors.toList());
        
        Map<BudgetCategory, Double> similarityScores = new HashMap<>();
        
        for (Transaction assignedTransaction : assignedTransactions) {
            double similarity = calculateTransactionSimilarity(transaction, assignedTransaction);
            if (similarity > 0.6) {
                BudgetCategory category = assignedTransaction.getBudgetCategory();
                similarityScores.merge(category, similarity, Double::max);
            }
        }
        
        if (!similarityScores.isEmpty()) {
            BudgetCategory bestMatch = Collections.max(similarityScores.entrySet(), 
                Map.Entry.comparingByValue()).getKey();
            double confidence = Math.min(0.7, similarityScores.get(bestMatch)); // Max confidence 0.7
            return new AssignmentStrategy("similarity", bestMatch, confidence);
        }
        
        return new AssignmentStrategy("similarity", null, 0.0);
    }
    
    /**
     * Calculate similarity between two transactions
     */
    private double calculateTransactionSimilarity(Transaction t1, Transaction t2) {
        double similarity = 0.0;
        int factors = 0;
        
        // Merchant name similarity
        if (t1.getMerchantName() != null && t2.getMerchantName() != null) {
            similarity += calculateStringSimilarity(t1.getMerchantName(), t2.getMerchantName()) * 0.4;
            factors++;
        }
        
        // Description similarity
        if (t1.getDescription() != null && t2.getDescription() != null) {
            similarity += calculateStringSimilarity(t1.getDescription(), t2.getDescription()) * 0.3;
            factors++;
        }
        
        // Amount similarity (within 20% range)
        double amountDiff = Math.abs(t1.getAmount().doubleValue() - t2.getAmount().doubleValue());
        double avgAmount = (Math.abs(t1.getAmount().doubleValue()) + Math.abs(t2.getAmount().doubleValue())) / 2;
        if (avgAmount > 0) {
            double amountSimilarity = Math.max(0, 1 - (amountDiff / (avgAmount * 0.2)));
            similarity += amountSimilarity * 0.2;
            factors++;
        }
        
        // Transaction type similarity
        if (t1.getTransactionType() == t2.getTransactionType()) {
            similarity += 0.1;
        }
        factors++;
        
        return factors > 0 ? similarity / factors : 0.0;
    }
    
    /**
     * Calculate string similarity using Levenshtein distance
     */
    private double calculateStringSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        
        s1 = s1.toLowerCase().trim();
        s2 = s2.toLowerCase().trim();
        
        if (s1.equals(s2)) return 1.0;
        if (s1.contains(s2) || s2.contains(s1)) return 0.8;
        
        int distance = levenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        return maxLength == 0 ? 1.0 : 1.0 - (double) distance / maxLength;
    }
    
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                        dp[i - 1][j] + 1,
                        Math.min(
                            dp[i][j - 1] + 1,
                            dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1)
                        )
                    );
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    private List<Transaction> getUnassignedTransactionsForMonth(User user, Integer month, Integer year) {
        List<Transaction> allUnassigned = transactionRepository.findByUserAndAssignmentStatus(
            user, Transaction.AssignmentStatus.UNASSIGNED);
        
        return allUnassigned.stream()
            .filter(transaction -> {
                int transactionMonth = transaction.getTransactionDate().getMonthValue();
                int transactionYear = transaction.getTransactionDate().getYear();
                return transactionMonth == month && transactionYear == year;
            })
            .collect(Collectors.toList());
    }
    
    private String mapTransactionCategoryToBudgetCategory(String transactionCategoryKey) {
        // Enhanced mapping logic with French and international categories
        switch (transactionCategoryKey) {
            // Basic categories (existing)
            case "categories.housing": return "budgetCategories.housing";
            case "categories.utilities": return "budgetCategories.utilities";
            case "categories.groceries": return "budgetCategories.groceries";
            case "categories.transportation": return "budgetCategories.transportation";
            case "categories.insurance": return "budgetCategories.insurance";
            case "categories.healthcare": return "budgetCategories.healthcare";
            case "categories.debtPayments": return "budgetCategories.debtPayments";
            case "categories.diningOut": case "categories.restaurants": return "budgetCategories.restaurants";
            case "categories.entertainment": return "budgetCategories.entertainment";
            case "categories.shopping": return "budgetCategories.shopping";
            case "categories.personalCare": return "budgetCategories.personalCare";
            case "categories.hobbies": return "budgetCategories.hobbies";
            case "categories.travel": return "budgetCategories.travel";
            case "categories.savings": return "budgetCategories.emergencyFund";
            case "categories.investments": return "budgetCategories.investments";
            case "categories.education": return "budgetCategories.education";
            case "categories.giftsDonations": return "budgetCategories.giftsDonations";
            case "categories.businessExpenses": return "budgetCategories.businessExpenses";
            case "categories.miscellaneous": return "budgetCategories.miscellaneous";
            
            // French-specific categories
            case "categories.loyer": case "categories.logement": return "budgetCategories.housing";
            case "categories.essence": case "categories.carburant": return "budgetCategories.gasoline";
            case "categories.epicerie": case "categories.supermarche": return "budgetCategories.groceries";
            case "categories.restaurant": case "categories.repas": return "budgetCategories.restaurants";
            case "categories.transport": case "categories.metro": return "budgetCategories.transportation";
            case "categories.assurance": case "categories.mutuelle": return "budgetCategories.insurance";
            case "categories.pharmacie": case "categories.sante": return "budgetCategories.healthcare";
            case "categories.impots": case "categories.taxes": return "budgetCategories.taxes";
            case "categories.banque": case "categories.fraisBancaires": return "budgetCategories.banking";
            case "categories.divertissement": case "categories.loisirs": return "budgetCategories.entertainment";
            case "categories.achats": case "categories.magasins": return "budgetCategories.shopping";
            
            // Gas/fuel specific mapping
            case "categories.gasoline": case "categories.fuel": return "budgetCategories.gasoline";
            
            default: return null;
        }
    }
    
    // Helper methods for creating static maps
    private static Map<String, Set<Pattern>> createCategoryPatterns() {
        Map<String, Set<Pattern>> patterns = new HashMap<>();
        
        patterns.put("budgetCategories.groceries", Set.of(
            // French supermarkets and grocery stores
            Pattern.compile("(?i).*(carrefour|leclerc|auchan|intermarche|super u|casino|monoprix|franprix).*"),
            Pattern.compile("(?i).*(lidl|aldi|picard|grand frais|match|cora).*"),
            Pattern.compile("(?i).*(epicerie|supermarche|alimentation|marche|boulangerie|fromagerie).*"),
            // International/English patterns
            Pattern.compile("(?i).*(grocery|supermarket|walmart|costco|whole foods|trader joe).*"),
            Pattern.compile("(?i).*(food|market|deli|bakery).*")
        ));
        
        patterns.put("budgetCategories.gasoline", Set.of(
            // French gas stations
            Pattern.compile("(?i).*(total|bp|esso|shell|agip|avia|station service).*"),
            Pattern.compile("(?i).*(carburant|essence|gazole|diesel|station).*"),
            // International patterns
            Pattern.compile("(?i).*(gas|fuel|exxon|chevron|mobil|texaco|petroleum|gasoline).*")
        ));
        
        patterns.put("budgetCategories.restaurants", Set.of(
            // French restaurants and food chains
            Pattern.compile("(?i).*(mcdonald|quick|kfc|burger king|subway|domino|pizza hut).*"),
            Pattern.compile("(?i).*(brasserie|bistro|cafe|restaurant|pizzeria|creperie).*"),
            Pattern.compile("(?i).*(deliveroo|uber eats|just eat|foodora|resto|repas|swile|up).*"),
            // International patterns
            Pattern.compile("(?i).*(dining|food delivery|starbucks|taco|coffee).*")
        ));
        
        patterns.put("budgetCategories.utilities", Set.of(
            // French utility companies
            Pattern.compile("(?i).*(edf|engie|gdf|orange|sfr|free|bouygues|veolia|suez).*"),
            Pattern.compile("(?i).*(electricite|gaz|eau|internet|telephone|mobile|facture).*"),
            // International patterns
            Pattern.compile("(?i).*(electric|power|gas company|water|sewer|utility|internet|cable|phone).*")
        ));
        
        patterns.put("budgetCategories.transportation", Set.of(
            // French transportation
            Pattern.compile("(?i).*(sncf|ratp|metro|bus|tram|uber|taxi|peage|parking).*"),
            Pattern.compile("(?i).*(transport|navigo|autoroute|station essence|reparation auto).*"),
            Pattern.compile("(?i).*(blablacar|ouigo|tgv|rer|velib).*"),
            // International patterns
            Pattern.compile("(?i).*(lyft|transit|toll|car wash|auto|vehicle|mechanic|repair).*")
        ));
        
        patterns.put("budgetCategories.healthcare", Set.of(
            // French healthcare
            Pattern.compile("(?i).*(pharmacie|docteur|medecin|hopital|clinique|dentiste|mutuelle).*"),
            Pattern.compile("(?i).*(secu|cpam|complementaire sante|ordonnance|medicament).*"),
            // International patterns
            Pattern.compile("(?i).*(pharmacy|doctor|medical|health|hospital|clinic|dental|prescription).*")
        ));
        
        patterns.put("budgetCategories.entertainment", Set.of(
            // French entertainment
            Pattern.compile("(?i).*(netflix|spotify|canal plus|ocs|disney|prime video|deezer).*"),
            Pattern.compile("(?i).*(cinema|theatre|spectacle|concert|fnac|jeu|abonnement).*"),
            // International patterns
            Pattern.compile("(?i).*(amazon prime|hulu|movie|theater|game|entertainment|music|streaming|subscription).*")
        ));
        
        patterns.put("budgetCategories.shopping", Set.of(
            // French retail stores
            Pattern.compile("(?i).*(amazon|fnac|darty|boulanger|decathlon|ikea|h&m|zara).*"),
            Pattern.compile("(?i).*(galeries lafayette|printemps|bon marche|auchan|carrefour).*"),
            Pattern.compile("(?i).*(magasin|boutique|achat|commerce|vente|shopping).*"),
            // International patterns
            Pattern.compile("(?i).*(ebay|target|bestbuy|apple store|clothing|fashion|retail|store|shop|mall|outlet).*")
        ));
        
        patterns.put("budgetCategories.insurance", Set.of(
            // French insurance companies
            Pattern.compile("(?i).*(axa|allianz|generali|maif|macif|matmut|maaf|groupama).*"),
            Pattern.compile("(?i).*(assurance|mutuelle|prime|police|couverture|cotisation).*"),
            // International patterns
            Pattern.compile("(?i).*(insurance|allstate|geico|state farm|progressive|premium|policy|coverage).*")
        ));
        
        patterns.put("budgetCategories.housing", Set.of(
            // French housing/rent related
            Pattern.compile("(?i).*(loyer|charges|syndic|copropriete|edf|gdf|eau).*"),
            Pattern.compile("(?i).*(immobilier|appartement|maison|proprietaire|bailleur).*"),
            // International patterns
            Pattern.compile("(?i).*(rent|mortgage|property|landlord|housing|utilities).*")
        ));
        
        patterns.put("budgetCategories.education", Set.of(
            // French education
            Pattern.compile("(?i).*(universite|ecole|college|lycee|formation|cours).*"),
            Pattern.compile("(?i).*(crous|bourses|frais scolaires|inscription|manuel).*"),
            // International patterns
            Pattern.compile("(?i).*(university|school|education|tuition|books|course).*")
        ));
        
        patterns.put("budgetCategories.miscellaneous", Set.of(
            // French miscellaneous
            Pattern.compile("(?i).*(poste|colis|courrier|banque|frais|commission).*"),
            Pattern.compile("(?i).*(divers|autre|inconnu|non classe).*"),
            // International patterns
            Pattern.compile("(?i).*(post office|mail|bank fee|misc|other|various|unknown).*")
        ));
        
        patterns.put("budgetCategories.taxes", Set.of(
            // French taxes and government
            Pattern.compile("(?i).*(impots|taxe|dgfip|tresor public|prefecture|mairie).*"),
            Pattern.compile("(?i).*(caf|pole emploi|urssaf|rsi|amende|contravention).*"),
            // International patterns
            Pattern.compile("(?i).*(tax|government|irs|municipal|fine|penalty).*")
        ));
        
        patterns.put("budgetCategories.banking", Set.of(
            // French banks
            Pattern.compile("(?i).*(bnp|credit agricole|societe generale|lcl|credit mutuel|banque populaire).*"),
            Pattern.compile("(?i).*(caisse epargne|hsbc|ing|revolut|n26|boursorama).*"),
            Pattern.compile("(?i).*(frais bancaires|commission|agios|cotisation carte).*"),
            // International patterns
            Pattern.compile("(?i).*(bank|banking|atm|withdrawal|transfer|wire|fee).*")
        ));
        
        return patterns;
    }
    
    private static Map<String, AmountRange> createAmountRanges() {
        Map<String, AmountRange> ranges = new HashMap<>();
        ranges.put("budgetCategories.groceries", new AmountRange(15.0, 150.0));        // French grocery shopping
        ranges.put("budgetCategories.gasoline", new AmountRange(20.0, 90.0));          // Gas station fill-ups in France
        ranges.put("budgetCategories.utilities", new AmountRange(30.0, 250.0));        // French utility bills
        ranges.put("budgetCategories.restaurants", new AmountRange(8.0, 80.0));        // Dining out in France
        ranges.put("budgetCategories.insurance", new AmountRange(50.0, 400.0));        // French insurance premiums
        ranges.put("budgetCategories.transportation", new AmountRange(5.0, 150.0));    // Metro tickets, parking, taxi
        ranges.put("budgetCategories.healthcare", new AmountRange(10.0, 200.0));       // Pharmacy, medical visits
        ranges.put("budgetCategories.shopping", new AmountRange(15.0, 300.0));         // Retail purchases
        ranges.put("budgetCategories.entertainment", new AmountRange(5.0, 50.0));      // Streaming, cinema tickets
        ranges.put("budgetCategories.housing", new AmountRange(300.0, 2000.0));        // Rent, housing costs in France
        ranges.put("budgetCategories.taxes", new AmountRange(50.0, 1500.0));           // French taxes and government fees
        ranges.put("budgetCategories.banking", new AmountRange(2.0, 50.0));            // Bank fees, card fees
        ranges.put("budgetCategories.education", new AmountRange(20.0, 500.0));        // Education costs
        return ranges;
    }
    
    // Helper classes
    public static class AssignmentResult {
        private final List<TransactionAssignment> assigned = new ArrayList<>();
        private final List<Transaction> needsReview = new ArrayList<>();
        
        public void addAssigned(Transaction transaction, String strategy, double confidence) {
            assigned.add(new TransactionAssignment(transaction, strategy, confidence));
        }
        
        public void addNeedsReview(Transaction transaction) {
            needsReview.add(transaction);
        }
        
        public List<TransactionAssignment> getAssigned() { return assigned; }
        public List<Transaction> getNeedsReview() { return needsReview; }
        
        public int getTotalAssigned() { return assigned.size(); }
        public int getTotalNeedsReview() { return needsReview.size(); }
        
        public Map<String, Integer> getStrategyBreakdown() {
            return assigned.stream()
                .collect(Collectors.groupingBy(
                    TransactionAssignment::getStrategy,
                    Collectors.summingInt(ta -> 1)
                ));
        }
    }
    
    public static class TransactionAssignment {
        private final Transaction transaction;
        private final String strategy;
        private final double confidence;
        
        public TransactionAssignment(Transaction transaction, String strategy, double confidence) {
            this.transaction = transaction;
            this.strategy = strategy;
            this.confidence = confidence;
        }
        
        public Transaction getTransaction() { return transaction; }
        public String getStrategy() { return strategy; }
        public double getConfidence() { return confidence; }
    }
    
    private static class AssignmentStrategy {
        private final String strategy;
        private final BudgetCategory budgetCategory;
        private final double confidence;
        
        public AssignmentStrategy(String strategy, BudgetCategory budgetCategory, double confidence) {
            this.strategy = strategy;
            this.budgetCategory = budgetCategory;
            this.confidence = confidence;
        }
        
        public boolean isSuccess() { return budgetCategory != null && confidence > 0.4; }
        public String getStrategy() { return strategy; }
        public BudgetCategory getBudgetCategory() { return budgetCategory; }
        public double getConfidence() { return confidence; }
    }
    
    private static class AmountRange {
        private final double min;
        private final double max;
        
        public AmountRange(double min, double max) {
            this.min = min;
            this.max = max;
        }
        
        public boolean contains(double amount) {
            return amount >= min && amount <= max;
        }
        
        public double getConfidenceForAmount(double amount) {
            if (!contains(amount)) return 0.0;
            
            double center = (min + max) / 2;
            double distance = Math.abs(amount - center);
            double maxDistance = (max - min) / 2;
            
            return 1.0 - (distance / maxDistance);
        }
    }
}