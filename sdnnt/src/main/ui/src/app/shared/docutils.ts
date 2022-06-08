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

    static hlavnizahlavi(doc: SolrDocument) {
        let str = this.first(doc.marc_100a);
        if (doc.marc_100b) {
            str = str+ ' ' + this.first(doc.marc_100b);
        }
        if (doc.marc_100c) {
            str = str + ' ' + this.first(doc.marc_100c);
        }
        if (doc.marc_100d) {
            str = str + ' ' + this.first(doc.marc_100d);
        }
        return str;
    }


    static vydani(doc: SolrDocument) {
        return this.first(doc.marc_250a);
    }

    
    static nakladatelskeUdaje(doc: SolrDocument) {
        let str =this.first(doc.marc_260a);
        if(doc.marc_260b) {
            str  = str +' '+this.first(doc.marc_260b);
        }
        if (doc.marc_260c) {
            str  = str +' '+this.first(doc.marc_260c);
        }
        return str;
        //return  this.first(doc.marc_264a) + ' '+this.first(doc.marc_264b)+ ' '+this.first(doc.this.first(doc.marc_264b));
    }

    static nakladatel(doc: SolrDocument) {
        let str =this.first(doc.marc_264a);
        if(doc.marc_264b) {
            str  = str +' '+this.first(doc.marc_264b);
        }
        if (doc.marc_264c) {
            str  = str +' '+this.first(doc.marc_264c);
        }
        return str;
    }
}