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

    private List<String> foundIdentifiers;
    private Map<String, Object> item;

    public ImportResult(List<String> foundIdentifiers, Map<String, Object> item) {
        this.foundIdentifiers = foundIdentifiers;
        this.item = item;
    }

    public List<String> getFoundIdentifiers() {
        return foundIdentifiers;
    }

    public Map<String, Object> getItem() {
        return item;
    }

    public  boolean found() {
        return item.containsKey("found") && item.containsKey("hits_na_vyrazeni") && ((Integer)item.get("hits_na_vyrazeni")> 0);
    }
}
