import { SolrDocument } from "./solr-document";

/** Doc title utils */
export class DocsUtils {

      // move to utils
    static first(arr) {
        return arr && arr[0] ? arr[0] : "";
    }

    static title(doc: SolrDocument) {
        return  this.first(doc.marc_245a) + ' '+this.first(doc.marc_245b)+' '+this.first(doc.marc_245n)+' '+ this.first(doc.marc_245p)+' '+ this.first(doc.marc_245c)+' '+ this.first(doc.marc_245i);
    }
}