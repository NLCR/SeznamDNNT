package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.services.LocalesService;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import java.util.Locale;

public class LocaleServiceImpl implements LocalesService {

    private Locale locale;

    public LocaleServiceImpl(HttpServletRequest request) {
        this.locale = request.getLocale();
    }

    public LocaleServiceImpl(ContainerRequestContext context) {
        this.locale = context.getLanguage();
    }

    @Override
    public Locale getLocale() {
        return locale;
    }
}
