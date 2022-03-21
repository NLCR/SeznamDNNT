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

    // if (resp.notifications) {
    //     this.docs.forEach(doc => {
    //       const identifier = doc.identifier;
    //       resp.notifications.forEach(z => {
    //         if (z.identifier.includes(identifier)) {
    //           doc.hasNotifications = true;
    //         }
    //       });
    //     });
    //   }


}