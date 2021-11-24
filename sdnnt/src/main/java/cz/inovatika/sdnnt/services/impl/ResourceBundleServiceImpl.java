package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.services.LocalesService;
import cz.inovatika.sdnnt.services.ResourceServiceService;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import java.util.ResourceBundle;

public class ResourceBundleServiceImpl implements ResourceServiceService {

    private static final String MESSAGES = "cz.inovatika.sdnnt.services.messages";

    private LocalesService localesService;

    public ResourceBundleServiceImpl(LocalesService localesService) {
        this.localesService = localesService;
    }

    public ResourceBundleServiceImpl(HttpServletRequest request) {
        this.localesService = new LocaleServiceImpl(request);
    }

    public ResourceBundleServiceImpl(ContainerRequestContext requestContext) {
        this.localesService = new LocaleServiceImpl(requestContext);
    }

    @Override
    public ResourceBundle getBundle() {
        ResourceBundle bundle = ResourceBundle.getBundle(MESSAGES, localesService.getLocale());
        return bundle;
    }
}
