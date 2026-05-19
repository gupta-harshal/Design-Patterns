
// This is a simple implementation of the Factory Design Pattern in Java.
interface Vehicle{
    void rent();
}

class Car implements Vehicle{
    @Override
    public void rent() {
        System.out.println("You have rented a car.");
    }
}

class Bike implements Vehicle{
    @Override
    public void rent() {
        System.out.println("You have rented a bike.");
    }
}

class Truck implements Vehicle{
    @Override
    public void rent() {
        System.out.println("You have rented a truck.");
    }
}

// This class is a simple factory that creates vehicles based on the type provided.
class VehicleFactory{
    public static Vehicle createVehicle(String type){
        switch (type.toLowerCase()){
            case "car":
                return new Car();
            case "bike":
                return new Bike();
            case "truck":
                return new Truck();
            default:
                throw new IllegalArgumentException("Unknown vehicle type: " + type);
        }
    }
}
public class Main {
    public static void main(String[] args) {
        // The client code uses the factory to create vehicles without needing to know the details of their creation.
        Vehicle car = VehicleFactory.createVehicle("car");
        car.rent();

        Vehicle bike = VehicleFactory.createVehicle("bike");
        bike.rent();

        Vehicle truck = VehicleFactory.createVehicle("truck");
        truck.rent();
    }
}