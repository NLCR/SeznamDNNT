import { SolrDocument } from "./solr-document";

/** Doc title utils */
export class SearchResultsUtils {

    enhanceByRequest(docs, zadosti) {
        docs.forEach(doc => {
            const identifier = doc.identifier;
            zadosti.forEach(z => {
                if (z.state !== 'processed') {
                    if (z.identifiers.includes(identifier)) {
                        doc.zadost = z;
                    }
                }  
            });
        });
    }

    enhanceByNotifications(docs, notifications) {
        docs.forEach(d => {
            notifications?.forEach(n=> {
            const identifier = d.identifier;
            if (n.identifier.includes(identifier)) {
                    d.hasNotifications = true;
                }
            });
        });

    }
    // rule based notfications
    enhanceByRulebasedNotifications(docs, notifications) {
        docs.forEach(d => {
            notifications?.forEach(n=> {
            const identifier = d.identifier;
            if (n.identifier.includes(identifier)) {
                    d.hasRuleNotifications = true;
                }
            });
        });
    }

}