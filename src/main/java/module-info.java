module com.javascene.gradingfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.net.http;
    requires java.logging;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires org.yaml.snakeyaml;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires static lombok;
    requires org.apache.commons.io;
    requires org.slf4j;

    opens com.javascene.gradingfx.config.property to org.yaml.snakeyaml, com.fasterxml.jackson.databind;
    opens com.javascene.gradingfx.enmu to com.fasterxml.jackson.databind;
    opens com.javascene.gradingfx to javafx.fxml;
    exports com.javascene.gradingfx;
    opens com.javascene.gradingfx.controller to javafx.fxml;
    opens com.javascene.gradingfx.model to javafx.base, com.fasterxml.jackson.databind;
    opens com.javascene.gradingfx.config.bean to com.fasterxml.jackson.databind;
}
