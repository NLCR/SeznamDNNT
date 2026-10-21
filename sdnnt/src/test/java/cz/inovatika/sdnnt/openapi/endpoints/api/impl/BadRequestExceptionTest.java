package cz.inovatika.sdnnt.openapi.endpoints.api.impl;

import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Test;

public class BadRequestExceptionTest {

    @Test
    public void defaultsToNothingToSaveReason() {
        DNNTRequestApiServiceImpl.BadRequestException exception =
                new DNNTRequestApiServiceImpl.BadRequestException(new ArrayList<>());

        Assert.assertEquals("Nothing to save !", exception.getReason());
    }

    @Test
    public void preservesSpecificValidationReason() {
        DNNTRequestApiServiceImpl.BadRequestException exception =
                new DNNTRequestApiServiceImpl.BadRequestException(
                        new ArrayList<>(), "Exceeded maximum items in one request");

        Assert.assertEquals("Exceeded maximum items in one request", exception.getReason());
    }
}
