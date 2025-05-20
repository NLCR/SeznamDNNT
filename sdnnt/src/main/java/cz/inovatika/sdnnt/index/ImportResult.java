/*
 * Copyright (C) 2025  Inovatika
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.inovatika.sdnnt.index;

import java.util.List;
import java.util.Map;


public class ImportResult {

    public static class EANIdent {

        private String eanIdent;
        public EANIdent(String ean) {
            this.eanIdent = ean;
        }
        public String getEanIdentifier() {
            return this.eanIdent;
        }
    }


    // vsechny identifikatory
    private List<String> foundIdentifiers;
    // item ziskany z importniho xml
    private Map<String, Object> item;

    private EANIdent eanIdent = null;

    public ImportResult(List<String> foundIdentifiers, Map<String, Object> item, String eanIdent) {
        this.foundIdentifiers = foundIdentifiers;
        this.item = item;
        if (eanIdent != null) {
            this.eanIdent = new EANIdent(eanIdent);
        }
    }

    public List<String> getFoundIdentifiers() {
        return foundIdentifiers;
    }

    public Map<String, Object> getItem() {
        return item;
    }

    public EANIdent getEanIdent() {
        return eanIdent;
    }

    public  boolean found() {
        return item.containsKey("found") && item.containsKey("hits_na_vyrazeni") && ((Integer)item.get("hits_na_vyrazeni")> 0);
    }
}
