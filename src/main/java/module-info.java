module com.javascene.gradingfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.dataformat.yaml;

    requires static lombok;
    requires org.slf4j;
    requires org.yaml.snakeyaml;

    requires java.net.http;
    requires java.logging;
    requires java.desktop;
    requires org.apache.commons.io;


    opens com.javascene.gradingfx to javafx.fxml;
    exports com.javascene.gradingfx;
    opens com.javascene.gradingfx.controller to javafx.fxml;
    opens com.javascene.gradingfx.model to javafx.base;
    exports com.javascene.gradingfx.model;
    exports com.javascene.gradingfx.enmu;
    opens com.javascene.gradingfx.config.property to com.fasterxml.jackson.databind;
}
