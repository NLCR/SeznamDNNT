package cz.inovatika.sdnnt.indexer.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NotificationInterval {
    den,
    tyden,
    mesic,
    none;

}
