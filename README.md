## Refactoring-safe POJO property names
[![Build Status](https://travis-ci.org/HBTGmbH/java-propertyname.svg?branch=master)](https://travis-ci.org/HBTGmbH/java-propertyname) [![Maven Snapshot](https://img.shields.io/nexus/s/https/oss.sonatype.org/de.hbt.propertyname/propertyname.svg)](https://oss.sonatype.org/content/repositories/snapshots/de/hbt/propertyname/propertyname/)

```Java
@lombok.Data class Car {
  Manufacturer manufacturer;
  Model model;
}
@lombok.Data class Manufacturer {
  Collection<Model> models;
}
@lombok.Data class Model {
  boolean suv;
  String name;
}

assertThat(nameOf(Car::getModel)).isEqualTo("model");
assertThat(name(of(Car::getModel).getName())).isEqualTo("model.name");
assertThat(name(of(Car::getModel).isSuv())).isEqualTo("model.suv");
assertThat(name(any(of(Car::getManufacturer).getModels()).getName()))
    .isEqualTo("manufacturer.models.name");
```
