package main;

public class Card {
    static int id = 0;
    private String type;
    private int number;

    Card(String type, int number){
        this.type = type;
        this.number = number;
        id++;
    }

    public void displayInfo(){
        IO.println("type: " + this.type);
        IO.println("number: " + this.number);
    }

}