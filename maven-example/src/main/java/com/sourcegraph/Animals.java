package com.sourcegraph;

public class Animals {
    interface Animal {
        String sound();
    }

    class Dog implements Animal {
        public String sound() { return "woof"; }
    }
    class Cat implements Animal {
        public String sound() { return "meow"; }
    }
  
}

