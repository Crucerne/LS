/*
 * AUTHOR:
 * Alno "Crucerne" Lau
 * DATE CREATED:
 * 03/02/16
 * LAST MODIFIED:
 * 03/06/16
 * DESCRIPTION:
 * This program act as a lottery simulator, random ticket generator, and various common related features.
 * The random ticket generator creates tickets in parallel threads, increasing performance and efficiency in terms of time.
 * There is also a recurrence limit the user can set that prevents duplicate tickets from being saved.
 * MILESTONES:
 * [x] - Create Powerball menu.
 * [x] - Random Powerball generators.
 * [x] - Overrides and Comparators for natural ordering of Powerball objects.
 * [x] - Read/Write methods for Powerball essentials.
 * [x] - Miscellaneous Powerball methods.
 * [ ] - Implement Mega Millions Lottery.
 * [ ] - Decide on other lotteries to implement.
*/
package com.crucerne.www.lottery;

import com.crucerne.www.lottery.generator.Powerball;

public class Lottery {

	public static void main(String[] args) {
				
		loadPowerballSaves();
		
		Powerball.helpOption();
		Powerball.powerballMenu();
	}

	public static void loadPowerballSaves()
	{
		Powerball.readRecurrenceLimit();
		Powerball.readWinningPowerball();
		Powerball.readRandomGeneratedTickets();
	}
}
