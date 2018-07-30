## Refactoring-safe POJO property names
[![Build Status](https://travis-ci.org/HBTGmbH/java-propertyname.svg?branch=master)](https://travis-ci.org/HBTGmbH/java-propertyname)

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
assertThat(name(() -> of(Car::getManufacturer).getModels()
    .forEach(Model::getName))).isEqualTo("manufacturer.models.name");
```
