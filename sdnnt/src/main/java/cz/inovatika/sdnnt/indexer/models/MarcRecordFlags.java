package cz.inovatika.sdnnt.indexer.models;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

import java.util.Date;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.FLAG_PUBLIC_IN_DL;


/**
 * Marc record flags
 * @author happy
 *
 */
public class MarcRecordFlags {

    // The title is public in digital library
    private boolean publicInDl = false;

    public MarcRecordFlags() {
    }

    public MarcRecordFlags(boolean publicInDl) {
        this.publicInDl = publicInDl;
    }

    public boolean isPublicInDl() {
        return publicInDl;
    }

    public void setPublicInDl(boolean publicInDl) {
        this.publicInDl = publicInDl;
    }

    public void enhanceDoc(SolrInputDocument sdoc) {
        if (isPublicInDl()) {
            sdoc.setField(FLAG_PUBLIC_IN_DL, isPublicInDl());
        } else {
            sdoc.remove(FLAG_PUBLIC_IN_DL);
        }
    }


    public static MarcRecordFlags fromSolrDoc(SolrDocument doc) {
        MarcRecordFlags flags = new MarcRecordFlags();
        if (doc.containsKey(FLAG_PUBLIC_IN_DL)) {
            flags.setPublicInDl((Boolean) doc.getFirstValue(FLAG_PUBLIC_IN_DL));
        }
        if (flags.isPublicInDl()) return flags;
        else return null;
    }
}
