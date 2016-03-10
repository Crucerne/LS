/*
 * AUTHOR:
 * Alno "Crucerne" Lau
 * LAST MODIFIED:
 * 03/06/16
 * DESCRIPTION:
 * Powerball lottery ticket generator and simulator.
 * The values for the lottery balls are based upon the ranges found at the official 
 * website on the last modified date. The possible ranges for the white and
 * red balls are currently:
 * white - 1 to 69
 * red - 1 to 26
 */

package com.crucerne.www.lottery.generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Powerball implements Serializable, Comparable<Powerball>, Runnable {

	// Serial Version ID Last Modified: 03/03/16
	private static final long serialVersionUID = 549747274699951680L;
	// Concurrent set collection used for storing randomly generated tickets.
	private volatile static ConcurrentSkipListSet<Powerball> s_powerball_tickets = new ConcurrentSkipListSet<Powerball>();
	// Maximum recurring numbers allowed in all randomly generated tickets.
	private static int s_recur_limit = 3;
	// Winning Powerball number set.
	private static Powerball s_winning_powerball = new Powerball();
	// String name for Powerball Settings file.
	private static final String S_POWERBALL_SETTINGS = "PowerballSettings.bin";
	// String name for winning Powerball file. 
	private static final String S_POWERBALL_WIN = "PowerballWin.bin";
	// String name for random Powerball tickets file.
	private static final String S_POWERBALL_RANDOM = "PowerballRandomTickets.bin";

	// Possible value ranges for Powerball's white and red balls.
	public static final int WHITE_MIN = 1;
	public static final int WHITE_MAX = 69;
	public static final int RED_MIN = 1;
	public static final int RED_MAX = 26;

	// Powerball fields
	private TreeSet<Integer> p_white_numbers = new TreeSet<Integer>();
	private int p_red_number = 1;

	// #REGION CONSTRUCTORS

	/*
	 * DESCRIPTION: Constructors for Powerball objects.1) The constructor that
	 * takes no arguments will instantiate a new Powerball object and assign a
	 * random white and red numbers to it. 2) Constructors that takes
	 * arguments(maximum of 6) will instantiate a Powerball object and assign
	 * the argument integer values as its white and red numbers with the 6th
	 * argument(if provided) being its red number. Any missing arguments will
	 * assign the default values of 2, 3, 4, 5, and 6 respectively. 3) The
	 * constructor that takes another Powerball object as argument will make a
	 * copy of other object by making a copy of the other Powerball's white and
	 * red numbers.
	 */
	public Powerball() {
		while (this.p_white_numbers.size() != 5) {
			this.p_white_numbers.add(ThreadLocalRandom.current().nextInt(WHITE_MAX) + WHITE_MIN);
		}
		this.p_red_number = ThreadLocalRandom.current().nextInt(RED_MAX) + RED_MIN;
	}

	public Powerball(Powerball pball) {
		this.p_white_numbers = new TreeSet<Integer>(pball.p_white_numbers);
		this.p_red_number = new Integer(pball.p_red_number);
	}

	public Powerball(int first) {
		this(first, 2, 3, 4, 5, 1);
	}

	public Powerball(int first, int second) {
		this(first, second, 3, 4, 5, 1);
	}

	public Powerball(int first, int second, int third) {
		this(first, second, third, 4, 5, 1);
	}

	public Powerball(int first, int second, int third, int fourth) {
		this(first, second, third, fourth, 5, 1);
	}

	public Powerball(int first, int second, int third, int fourth, int fifth) {
		this(first, second, third, fourth, fifth, 1);
	}

	public Powerball(int first, int second, int third, int fourth, int fifth, int sixth) {
		this.p_white_numbers.add(first);
		this.p_white_numbers.add(second);
		this.p_white_numbers.add(third);
		this.p_white_numbers.add(fourth);
		this.p_white_numbers.add(fifth);
		this.p_red_number = sixth;
	}
	// #END CONSTRUCTORS

	// #REGION STATIC METHODS

	/*
	 * DESCRIPTION: Static method that sets the recurrence limit for all
	 * tickets. AFFECTED FIELDS: 1) s_recur_limit REQUIRED ARGUMENTS: 1) int
	 * new_limit - an integer that will be the new value for the recurrence
	 * limit("S_recur_limit").
	 */
	public static void setRecurLimit(int new_limit) {
		s_recur_limit = new_limit;
	}

	/*
	 * DESCRIPTION: Static method that returns the recurrence limit of
	 * tickets("s_recur_limit"). AFFECTED FIELDS: None. REQUIRED ARGUMENTS:
	 * None.
	 */
	public static int getRecurLimit() {
		return s_recur_limit;
	}

	/*
	 * DESCRIPTION: Static method that serves as the central hub for all
	 * Powerball lottery activities and methods. This method takes the user
	 * input and execute a task depending on the input. For all possible valid
	 * inputs and details, look at the "helpOption" method description. AFFECTED
	 * FIELDS: None. REQUIRED ARGUMENTS: None.
	 */
	public static void powerballMenu() {
		StringBuilder choice = new StringBuilder();
		Scanner scanner = new Scanner(System.in);
		while (!choice.toString().equals("e")) {

			choice.setLength(0);
			choice.append(scanner.nextLine().toString());

			// Generate a random winning Powerball option.
			if (choice.toString().equals("g")) {
				Powerball.winningNumberGeneratorOption();
				System.out.println("A new winning Powerball has been generated: " + Powerball.s_winning_powerball);
				Powerball.writeWinningPowerball();
			}
			// Generate user specified amount of random Powerball tickets
			// option.
			else if (choice.toString().equals("r")) {
				System.out.println("Enter the amount of tickets to generate:\n");
				int desired_amount;
				try {
					@SuppressWarnings("resource")
					Scanner int_scanner = new Scanner(System.in);
					desired_amount = int_scanner.nextInt();
					if (desired_amount < 1 || desired_amount > 30000) {
						System.out.println("Invalid input; value must be greater than 0 and less than 30,001.");
					} else if (desired_amount <= Powerball.s_powerball_tickets.size()) {
						System.out.println(
								"Desired amount of tickets is less than or equal to the current number of generated tickets. "
										+ "Clearing previously generated tickets...");
						Powerball.s_powerball_tickets.clear();
						System.out.println("Computing...");
						Powerball.parallelRandomTicketGeneratorOption(desired_amount);
						System.out.println("Tickets generation complete.");
						Powerball.writeRandomGeneratedTickets();
					} else {
						System.out.println("Computing...");
						Powerball.parallelRandomTicketGeneratorOption(desired_amount,
								desired_amount - Powerball.s_powerball_tickets.size());
						System.out.println("Tickets generation complete.");
						Powerball.writeRandomGeneratedTickets();
					}
				} catch (Exception e) {
					System.out.println("ERROR: Unexpected IO issue occurred!");
				}
			}
			// Show winning Powerball option.
			else if (choice.toString().equals("w")) {
				System.out.print("The current winning Powerball is: ");
				Powerball.showWinningNumberOption();
			}
			// View all randomly generated tickets option.
			else if (choice.toString().equals("v")) {
				Powerball.showRandomGeneratedTicketsOption();
			}
			// Delete all saved tickets option.
			else if (choice.toString().equals("d")) {
				Powerball.deleteRandomTicketsOption();
				System.out.println("All generated tickets has been cleared.");
			}
			// Set recurrence limit option.
			else if (choice.toString().equals("l")) {
				System.out.println("The current recurrence limit is: " + Powerball.getRecurLimit());
				System.out.println("Enter a new interger(3, 4, or 5) as the new recurrence limit:");
				int new_limit;
				try {
					@SuppressWarnings("resource")
					Scanner limit_scanner = new Scanner(System.in);
					new_limit = limit_scanner.nextInt();
					if (new_limit < 3 || new_limit > 5) {
						System.out.println(
								"Invalid input; value must be an integer 3, 4, or 5." + " Returning to main menu.");
					} else {
						Powerball.setRecurLimit(new_limit);
						System.out.println("Recurrence limit has been set to " + new_limit + ".");
						Powerball.writeRecurrenceLimit();
					}
				} catch (Exception e) {
					System.out.println("ERROR: Unexpected IO issue occurred!");
				}
			}
			// Help option.
			else if (choice.toString().equals("h")) {
				Powerball.helpOption();
			}
		}
		// Exit option
		System.out.println("Exiting Powerball lottery simulator.");
		scanner.close();
	}

	/*
	 * DESCRIPTION: Static method that lists the available commands for the user
	 * to enter and describes what each command does. AFFECTED FIELDS: None.
	 * REQUIRED ARGUMENTS: None.
	 */
	public static void helpOption() {
		System.out.println("Enter the letter for the corresponding command from the list:");
		System.out.println("[g] - Generate a new random winning Powerball number set.");
		System.out.println("[r] - Generate user specified random Powerball tickets.");
		System.out.println("[w] - Show winning Powerball.");
		System.out.println("[v] - View previously saved tickets.");
		System.out.println("[d] - Delete all saved tickets.");
		System.out.println("[l] - Set new number recurrence limit.");
		System.out.println("[h] - View command list.");
		System.out.println("[e] - Exit Powerball lottery.\n");
	}

	/*
	 * DESCRIPTION: Static method that generates a random Powerball and stores
	 * it into the class variable, "s_winning_powerball". If
	 * "s_winning_powerball" already contains a number set, this method will
	 * clear the existing number set and assign it a new number set. AFFECTED
	 * FIELDS: 1) s_winning_powerball REQUIRED ARGUMENTS: None.
	 */
	public static void winningNumberGeneratorOption() {
		Random random = new Random();
		if (s_winning_powerball != null) {
			s_winning_powerball.p_white_numbers.clear();
		}
		while (s_winning_powerball.p_white_numbers.size() != 5) {
			s_winning_powerball.p_white_numbers.add(random.nextInt(WHITE_MAX) + WHITE_MIN);
		}
		s_winning_powerball.p_red_number = random.nextInt(RED_MAX) + RED_MIN;
	}

	/*
	 * DEPRECATED 03/06/16 DESCRIPTION: Static method that generates a specified
	 * amount of Powerball tickets in multithreading. If recurring tickets are
	 * generated, the method will call the overloaded method of itself until the
	 * desired amount of tickets has been achieved. AFFECTED FIELDS: 1)
	 * s_powerball_tickets REQUIRED ARGUMENTS: int number_of_tickets - the
	 * amount of tickets the user wishes to generate.
	 */
	public static void parallelRandomTicketGeneratorOption(int number_of_tickets) {
		ExecutorService executor = Executors.newFixedThreadPool(number_of_tickets);
		for (int i = 0; i < number_of_tickets; i++) {
			executor.submit(new Powerball());
		}
		executor.shutdown();
		try {
			executor.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			System.out.println("ERROR: Thread pool exceeded allotted time.");
		}
		if (s_powerball_tickets.size() < number_of_tickets) {
			Powerball.parallelRandomTicketGeneratorOption(number_of_tickets,
					number_of_tickets - s_powerball_tickets.size());
		}
	}

	/*
	 * DESCRIPTION: Recursive static method that generates a specified amount of
	 * Powerball tickets in multithreading. If recurring tickets are generated,
	 * the method will call itself until the desired amount of tickets has been
	 * met. AFFECTED FIELDS: 1) s_powerball_tickets REQUIRED ARGUMENTS: 1) int
	 * number_of_tickets - the amount of tickets the user wishes to generate. 2)
	 * int tickets_remaining - the number of tickets still needed to be
	 * generated for the goal to be met.
	 */
	public static void parallelRandomTicketGeneratorOption(int number_of_tickets, int tickets_remaining) {
		ExecutorService executor = Executors.newFixedThreadPool(tickets_remaining);
		for (int i = 0; i < tickets_remaining; i++) {
			executor.submit(new Powerball());
		}
		executor.shutdown();
		try {
			executor.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			System.out.println("ERROR: Thread pool exceeded allotted time.");
		}
		if (s_powerball_tickets.size() < number_of_tickets) {
			Powerball.parallelRandomTicketGeneratorOption(number_of_tickets,
					number_of_tickets - s_powerball_tickets.size());
		}
	}

	/*
	 * DESCRIPTION: Static method that shows the current winning Powerball. If a
	 * winning Powerball does not exist, a message will be displayed to the
	 * user. AFFECTED FIELDS: None. REQUIRED ARGUMENTS: None.
	 */
	public static void showWinningNumberOption() {
		if (!s_winning_powerball.p_white_numbers.isEmpty()) {
			System.out.println(s_winning_powerball.toString());
		} else {
			System.out.println("A winning Powerball has not been retrieved/generated yet!");
		}
	}

	/*
	 * DESCRIPTION: Static method that displays the randomly generated tickets.
	 * If no tickets has been generated yet, a message will be displayed to the
	 * user. AFFECTED FIELDS: None. REQUIRED ARGUMENTS: None.
	 */
	public static void showRandomGeneratedTicketsOption() {
		if (!s_powerball_tickets.isEmpty()) {
			for (Powerball pball : s_powerball_tickets) {
				System.out.println(pball);
			}
			System.out.println("The current number of tickets is: " + s_powerball_tickets.size());
		} else {
			System.out.println("No tickets generated yet!");
		}
	}

	/*
	 * DESCRIPTION: Static method that deletes all current and saved randomly
	 * generated tickets. AFFECTED FIELDS: s_powerball_tickets. REQUIRED
	 * ARGUMENTS: None.
	 */
	public static void deleteRandomTicketsOption() {
		Powerball.s_powerball_tickets.clear();
		Powerball.writeRandomGeneratedTickets();
	}

	// #REGION READ/WRITE METHODS

	/*
	 * DESCRIPTION: Static method that reads the recurrence limit saved from the
	 * Powerball settings file. AFFECTED FIELDS: s_recur_limit. REQUIRED
	 * ARGUMENTS/FIELDS: String S_POWERBALL_SETTINGS - this string stores the
	 * name of the file used for reading the integer.
	 */
	public static void readRecurrenceLimit() {
		File file = new File(S_POWERBALL_SETTINGS);
		if (file.exists()) {
			try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(S_POWERBALL_SETTINGS))) {
				Powerball.s_recur_limit = ois.readInt();
			} catch (FileNotFoundException e) {
				System.out.println("ERROR: File not found!");
			} catch (IOException e) {
				System.out.println("ERROR: Unexpected issue occurred while reading recurrence limit from file!");
			}
		}
	}

	/*
	 * DESCRIPTION: Static method that writes the recurrence limit to the
	 * Powerball settings file. AFFECTED FIELDS: None. REQUIRED
	 * ARGUMENTS/FIELDS: String S_POWERBALL_SETTINGS - this string stores the
	 * name of the file used for writing the integer.
	 */
	public static void writeRecurrenceLimit() {
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(S_POWERBALL_SETTINGS, false))) {
			oos.writeInt(Powerball.getRecurLimit());
		} catch (FileNotFoundException e) {
			System.out.println("ERROR: File not found!");
		} catch (IOException e) {
			System.out.println("ERROR: Unexpected issue occurred while writing recurrence limit to file!");
		}
	}

	/*
	 * DESCRIPTION: Static method that reads the winning Powerball saved from
	 * the winning Powerball file. AFFECTED FIELDS: s_winning_powerball.
	 * REQUIRED ARGUMENTS/FIELDS: String S_POWERBALL_WIN - this string stores
	 * the name of the file used for reading the Powerball object.
	 */
	public static void readWinningPowerball() {
		File file = new File(S_POWERBALL_WIN);
		if (file.exists()) {
			try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(S_POWERBALL_WIN))) {
				Powerball.s_winning_powerball = (Powerball) ois.readObject();
			} catch (FileNotFoundException e) {
				System.out.println("ERROR: File not found!");
			} catch (IOException e) {
				System.out.println("ERROR: Unexpected issue occurred while reading winning Powerball from file!");
			} catch (ClassNotFoundException e) {
				System.out.println("ERROR: Failed to load data from file!");
			}
		}
	}

	/*
	 * DESCRIPTION: Static method that writes the winning Powerball to the
	 * winning Powerball file. AFFECTED FIELDS: None. REQUIRED ARGUMENTS/FIELDS:
	 * String S_POWERBALL_WIN - this string stores the name of the file used for
	 * writing the Powerball object.
	 */
	public static void writeWinningPowerball() {
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(S_POWERBALL_WIN, false))) {
			oos.writeObject(Powerball.s_winning_powerball);
		} catch (FileNotFoundException e) {
			System.out.println("ERROR: File not found!");
		} catch (IOException e) {
			System.out.println("ERROR: Unexpected issue occurred while writing winning Powerball to file!");
		}
	}

	/*
	 * DESCRIPTION: Static method that reads the Powerball objects saved from
	 * the random Powerball tickets file. AFFECTED FIELDS: s_powerball_tickets.
	 * REQUIRED ARGUMENTS/FIELDS: String S_POWERBALL_RANDOM - this string stores
	 * the name of the file used for reading the Powerball objects.
	 */
	@SuppressWarnings("unchecked")
	public static void readRandomGeneratedTickets() {
		File file = new File(S_POWERBALL_RANDOM);
		if (file.exists()) {
			try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(S_POWERBALL_RANDOM))) {
				Powerball.s_powerball_tickets = (ConcurrentSkipListSet<Powerball>) ois.readObject();
			} catch (FileNotFoundException e) {
				System.out.println("ERROR: File not found!");
			} catch (IOException e) {
				System.out.println(
						"ERROR: Unexpected issue occurred while reading randomly generated tickets from file!");
			} catch (ClassNotFoundException e) {
				System.out.println("ERROR: Failed to load data from file!");
			}
		}
	}

	/*
	 * DESCRIPTION: Static method that writes all randomly generated Powerball
	 * tickets to the random Powerball tickets file. AFFECTED FIELDS:
	 * s_powerball_tickets. REQUIRED ARGUMENTS/FIELDS: String S_POWERBALL_RANDOM
	 * - this string stores the name of the file used for writing the Powerball
	 * objects.
	 */
	public static void writeRandomGeneratedTickets() {
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(S_POWERBALL_RANDOM, false))) {
			// oos.writeInt(Powerball.s_powerball_tickets.size());
			oos.writeObject(Powerball.s_powerball_tickets);
		} catch (FileNotFoundException e) {
			System.out.println("ERROR: File not found!");
		} catch (IOException e) {
			System.out.println("ERROR: Unexpected issue occurred while writing randomly generated tickets to file!");
		}
	}

	// #END READ/WRITE METHODS

	// #END STATIC METHODS

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((p_white_numbers == null) ? 0 : p_white_numbers.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Powerball other = (Powerball) obj;
		if (p_white_numbers == null) {
			if (other.p_white_numbers != null)
				return false;
		}
		Powerball temp1 = new Powerball(this);
		temp1.p_white_numbers.retainAll(other.p_white_numbers);
		if (temp1.p_white_numbers.size() < s_recur_limit)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return p_white_numbers + " [" + p_red_number + "]";
	}

	@Override
	public void run() {
		Powerball.s_powerball_tickets.add(this);
	}

	@Override
	public int compareTo(Powerball pball) {
		int count = 0;
		Iterator<Integer> thisIterator = this.p_white_numbers.iterator();
		Iterator<Integer> otherIterator = pball.p_white_numbers.iterator();
		int thisInt = 0;
		int otherInt = 0;
		while (count != Powerball.s_recur_limit) {
			thisInt = thisIterator.next();
			otherInt = otherIterator.next();
			if (thisInt < otherInt) {
				return -1;
			} else if (thisInt > otherInt) {
				return 1;
			} else {
				count++;
			}
		}
		return 0;
	}

}
