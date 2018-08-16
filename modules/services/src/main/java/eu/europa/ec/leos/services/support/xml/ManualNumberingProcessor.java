/*
 * Copyright 2018 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.services.support.xml;

import static eu.europa.ec.leos.services.support.xml.VTDUtils.setupVTDNav;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.toByteArray;
import static java.nio.charset.StandardCharsets.UTF_8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ximpleware.VTDNav;
import com.ximpleware.XMLModifier;

import eu.europa.ec.leos.domain.common.InstanceContext;
import eu.europa.ec.leos.services.support.flow.Workflow;

@Component
@Workflow(InstanceContext.Type.COUNCIL)
class ManualNumberingProcessor implements NumberProcessor {

    private static final String DEFAULT_ART_NUM = "Article #";
    private static final String DEFAULT_REC_NUM = "(#)";
    private static final String NUM = "num";
    
    @Autowired
    private VtdXmlContentProcessor vtdXmlContentProcessor;

    private static final Logger LOG = LoggerFactory.getLogger(ManualNumberingProcessor.class);

    @Override
    public byte[] renumberArticles(byte[] xmlContent, String language) {
        return xmlContent;
    }

    @Override
    public String renumberImportedArticle(String xmlContent) {
        LOG.trace("Start renumberImportedArticle ");
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent.getBytes(UTF_8), false);
            XMLModifier xmlModifier = new XMLModifier(vtdNav);
            if (vtdNav.toElement(VTDNav.FC, NUM)) {
                int numIndex = vtdNav.getText();
                if (numIndex != -1) {
                    xmlModifier.updateToken(numIndex, DEFAULT_ART_NUM);
                }
            }
            return new String(toByteArray(xmlModifier));
        } catch (Exception e) {
            throw new RuntimeException("Unable to renumber Imported Article", e);
        }
    }

    @Override
    public byte[] renumberRecitals(byte[] xmlContent) {
        return vtdXmlContentProcessor.doXMLPostProcessing(xmlContent);
    }

    @Override
    public String renumberImportedRecital(String xmlContent) {
        LOG.trace("Start renumberImportedRecital ");
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent.getBytes(UTF_8), false);
            XMLModifier xmlModifier = new XMLModifier(vtdNav);
            if (vtdNav.toElement(VTDNav.FC, NUM)) {
                int numIndex = vtdNav.getText();
                if (numIndex != -1) {
                    xmlModifier.updateToken(numIndex, DEFAULT_REC_NUM);
                }
            }
            return new String(toByteArray(xmlModifier));
        } catch (Exception e) {
            throw new RuntimeException("Unable to renumber Imported recital", e);
        }
    }
}
