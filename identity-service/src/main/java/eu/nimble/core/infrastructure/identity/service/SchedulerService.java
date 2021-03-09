package eu.nimble.core.infrastructure.identity.service;


import eu.nimble.core.infrastructure.identity.clients.IndexingClientController;
import eu.nimble.core.infrastructure.identity.config.NimbleConfigurationProperties;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.mail.EmailService;
import eu.nimble.core.infrastructure.identity.mail.model.SubscriptionSummary;
import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.core.infrastructure.identity.repository.UaaUserRepository;
import eu.nimble.core.infrastructure.identity.system.ControllerUtils;
import eu.nimble.core.infrastructure.identity.uaa.KeycloakAdmin;
import eu.nimble.core.infrastructure.identity.utils.UblUtils;
import eu.nimble.service.model.solr.Search;
import eu.nimble.service.model.solr.item.ItemType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ProductPublishSubscriptionType;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;
import eu.nimble.utility.validation.NimbleRole;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class SchedulerService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${nimble.default-token}")
    private String defaultToken;

    @Autowired
    private EmailService emailService;
    @Autowired
    private IdentityService identityService;

    @Autowired
    private IndexingClientController indexingController;
    @Autowired
    private UaaUserRepository uaaUserRepository;
    @Autowired
    private KeycloakAdmin keycloakAdmin;
    @Autowired
    private PartyRepository partyRepository;

    /**
     * Cron job running every day at 6 AM to notify companies about their subscriptions.
     * It will send an email which includes the urls for product details if there are some products which are published
     * in last 24 hours.
     * */
    @Scheduled(cron = "0 0 6 ? * *")
    @Transactional
    public void notifyCompaniesForSubscriptions() {
        // run the scheduler if and only if the default token is provided
        if (!defaultToken.contentEquals("")) {
            logger.info("Running cron job to send emails to users for the subscriptions");
            // get the date of yesterday
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -1);
            String yesterday = dateFormat.format(cal.getTime());

            // retrieve products published in the last 24 hours
            eu.nimble.service.model.solr.SearchResult sr = indexingController.getNimbleIndexClient().searchItem(getSearchObjectForItems(yesterday), defaultToken);
            List<Object> items = sr.getResult();

            if (items.size() > 0) {
                // retrieve all parties with subscriptions
                List<PartyType> parties = partyRepository.findAllWithSubscriptions();
                // retrieve the subscribed category uris
                Set<String> categoryUris = new HashSet<>();
                for (PartyType party : parties) {
                    if (party.getProductPublishSubscription().getCategoryCode() != null) {
                        categoryUris.addAll(party.getProductPublishSubscription().getCategoryCode().stream().map(codeType -> codeType.getValue()).collect(Collectors.toSet()));
                    }
                }
                // retrieve the labels of subscribed categories
                Map<String, String> categoryLabelMap = new HashMap<>();
                sr = indexingController.getNimbleIndexClient().searchClass(getSearchObjectForCategories(categoryUris), defaultToken);
                List<Object> categories = sr.getResult();
                for (Object category : categories) {
                    LinkedHashMap<String, Object> lmap = (LinkedHashMap<String, Object>) category;
                    Map<String, String> languageMap = (Map<String, String>) lmap.get("label");
                    String uri = (String) lmap.get("uri");
                    String label = languageMap.get(NimbleConfigurationProperties.LanguageID.ENGLISH.toString());
                    categoryLabelMap.put(uri, label);
                }
                for (PartyType party : parties) {
                    // find the vat number for the party
                    // it is required to check whitelist/blacklist for the company
                    String vatNumber = getPartyVatNumber(party);
                    // find the subscribed products for the company
                    List<ItemType> products = getSubscribedProducts(vatNumber, items, party.getProductPublishSubscription());
                    if (products.size() > 0) {
                        try {
                            // create the subscription summaries for company
                            List<SubscriptionSummary> subscriptionMailModels = new ArrayList<>();
                            // check company subscriptions
                            for (String companyId : party.getProductPublishSubscription().getCompanyID()) {
                                List<String> catalogueIds = products.stream().filter(itemType -> itemType.getManufacturerId().contentEquals(companyId))
                                        .map(itemType -> itemType.getCatalogueId())
                                        .collect(Collectors.toList());

                                List<String> productIds = products.stream().filter(itemType -> itemType.getManufacturerId().contentEquals(companyId))
                                        .map(itemType -> itemType.getManufactuerItemId())
                                        .collect(Collectors.toList());

                                if (productIds.size() > 0) {
                                    PartyType company = partyRepository.findByHjid(Long.valueOf(companyId)).stream().findFirst().orElseThrow(ControllerUtils.CompanyNotFoundException::new);
                                    String companyName = UblUtils.getName(company.getPartyName(), NimbleConfigurationProperties.LanguageID.ENGLISH);

                                    subscriptionMailModels.add(new SubscriptionSummary(companyName, null, catalogueIds, productIds));
                                }

                            }
                            // check category subscriptions
                            for (CodeType codeType : party.getProductPublishSubscription().getCategoryCode()) {
                                List<String> catalogueIds = products.stream().filter(itemType -> itemType.getClassificationUri().contains(codeType.getValue()))
                                        .map(itemType -> itemType.getCatalogueId())
                                        .collect(Collectors.toList());

                                List<String> productIds = products.stream().filter(itemType -> itemType.getClassificationUri().contains(codeType.getValue()))
                                        .map(itemType -> itemType.getManufactuerItemId())
                                        .collect(Collectors.toList());

                                if (productIds.size() > 0) {
                                    subscriptionMailModels.add(new SubscriptionSummary(null, categoryLabelMap.get(codeType.getValue()), catalogueIds, productIds));
                                }

                            }
                            // enrich party with roles
                            identityService.enrichWithRoles(party);
                            // retrieve the email addresses of users
                            List<String> emails = new ArrayList<>();
                            if (party.getPerson() != null) {
                                // users with roles Monitor or Purchaser
                                emails = party.getPerson().stream().filter(personType -> personType.getContact() != null && personType.getContact().getElectronicMail() != null)
                                        .filter(personType -> personType.getRole().contains(NimbleRole.MONITOR.getName()) || personType.getRole().contains(NimbleRole.PURCHASER.getName()))
                                        .map(personType -> personType.getContact().getElectronicMail()).collect(Collectors.toList());
                                if (emails.size() == 0) {
                                    // users with roles Company Admin, Legal Representative or Initial representative
                                    emails = party.getPerson().stream().filter(personType -> personType.getContact() != null && personType.getContact().getElectronicMail() != null)
                                            .filter(personType -> personType.getRole().contains(NimbleRole.COMPANY_ADMIN.getName()) || personType.getRole().contains(NimbleRole.LEGAL_REPRESENTATIVE.getName()) || personType.getRole().contains(NimbleRole.INITIAL_REPRESENTATIVE.getName()))
                                            .map(personType -> personType.getContact().getElectronicMail()).collect(Collectors.toList());
                                }
                            }
                            // send email
                            if (emails.size() > 0) {
                                emailService.sendSubscriptionSummary(emails, subscriptionMailModels, "en");
                            }
                        } catch (Exception e) {
                            logger.error("Failed to send a subcription email to party: {}",party.getPartyIdentification().get(0).getID(), e);
                        }
                    }
                }
            }

            logger.info("Completed the cron job to send emails to users for the subscriptions");
        }
    }

    // helper functions

    /**
     * Returns {@link Search} for the items based on the given date.
     * It includes the query which searches for the items published in last 24 hours
     * @param yesterday the date of yesterday
     * @return {@link Search} including the proper query to retrieve items published in last 24 hours
     * */
    private eu.nimble.service.model.solr.Search getSearchObjectForItems(String yesterday) {
        eu.nimble.service.model.solr.Search search = new Search();
        search.setRows(Integer.MAX_VALUE);
        search.setQuery(String.format("creationDate:[%s TO NOW]", yesterday));
        return search;
    }

    /**
     * Returns {@link Search} for the given category uris.
     * @param categoryUris the list of category uris
     * @return {@link Search} including the proper query to retrieve specified categories
     * */
    private eu.nimble.service.model.solr.Search getSearchObjectForCategories(Set<String> categoryUris) {
        eu.nimble.service.model.solr.Search search = new Search();
        search.setRows(Integer.MAX_VALUE);
        search.setQuery(categoryUris.stream().map(s -> String.format("id:\"%s\"", s)).collect(Collectors.joining(" OR ")));
        return search;
    }

    /**
     * Returns the vat number of party.
     * @param party the party
     * @return the vat number of party if exists, otherwise, it returns null.
     * */
    private String getPartyVatNumber(PartyType party) {
        if (party.getPerson() != null) {
            for (PersonType person : party.getPerson()) {
                Optional<UaaUser> uaaUser = uaaUserRepository.findByUblPerson(person).stream().findFirst();
                if (uaaUser.isPresent()) {
                    Map<String, List<String>> attributes = keycloakAdmin.getUserResource(uaaUser.get().getExternalID()).toRepresentation().getAttributes();
                    if (attributes != null && attributes.containsKey("vatin")) {
                        return attributes.get("vatin").get(0);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the subscribed products for the given subscription.
     * Vat number is required to handle whitelist/blacklist functionality.
     * @param vatNumber the vat number of company.
     * @param items the list of Solr Items
     * @param productPublishSubscriptionType the company subscription
     * @return the list of subscribed products
     * */
    private List<ItemType> getSubscribedProducts(String vatNumber, List<Object> items, ProductPublishSubscriptionType productPublishSubscriptionType) {
        // retrieve subscribed category ids
        List<String> subscribedCategoryIds = productPublishSubscriptionType.getCategoryCode().stream().map(codeType -> codeType.getValue()).collect(Collectors.toList());

        List<ItemType> products = new ArrayList<>();
        // for each item, check whether it is subscribed or not
        for (Object item : items) {
            LinkedHashMap<String, Object> lmap = (LinkedHashMap<String, Object>) item;
            List<String> restrictedParties = (List<String>) lmap.get("restrictedParties");
            List<String> permittedParties = (List<String>) lmap.get("permittedParties");
            List<String> classificationUri = (List<String>) lmap.get("classificationUri");
            String manufacturerId = (String) lmap.get("manufacturerId");
            String manufacturerItemId = (String) lmap.get("manufactuerItemId");
            String catalogueId = (String) lmap.get("catalogueId");
            // consider the whitelist/blacklist of product, manufacturer id and product categories
            if ((restrictedParties == null || !restrictedParties.contains(vatNumber)) && (permittedParties == null || permittedParties.contains(vatNumber))
                    && (productPublishSubscriptionType.getCompanyID().contains(manufacturerId) || CollectionUtils.intersection(subscribedCategoryIds, classificationUri).size() > 0)) {
                ItemType itemType = new ItemType();
                itemType.setClassificationUri(classificationUri);
                itemType.setCatalogueId(catalogueId);
                itemType.setManufactuerItemId(manufacturerItemId);
                itemType.setManufacturerId(manufacturerId);
                products.add(itemType);
            }
        }

        return products;
    }
}