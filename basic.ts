abstract class Animal {
    abstract public sound(): string
}

class Dog extends Animal {
    public sound(): string {
        return "woof"
    }
}
class Cat extends Animal {
    public sound(): string {
        return "meow"
    }
}
