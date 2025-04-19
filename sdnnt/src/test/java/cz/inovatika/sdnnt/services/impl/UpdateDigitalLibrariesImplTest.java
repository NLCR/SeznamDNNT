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
package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.services.kraminstances.CheckKrameriusConfiguration;
import cz.inovatika.sdnnt.utils.LinksUtilities;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class UpdateDigitalLibrariesImplTest {


    @Test
    public void testCheck() {
        List<String> links = Arrays.asList("https://kramerius.cbvk.cz/uuid/uuid:b861d6b7-0ed4-4d37-b670-eb88c7204caa");
        Options options = Options.getInstance();
        CheckKrameriusConfiguration checkKram = CheckKrameriusConfiguration.initConfiguration(options.getJSONObject("check_kramerius"));
        List<String> digitalizedKeys = LinksUtilities.digitalizedKeys(checkKram, links);
        Assert.assertTrue(digitalizedKeys.size() > 0);
    }
}
