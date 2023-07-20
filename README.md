Project for the Data Structures and Algorithms course at UniBo 2022-2023, involving the creation of a player for generalised Connect Four able to pick each move within a time limit.
L0 and L1 are pre-made players provided by the professor; L0 randomly selects a move each turn, L1 does the same unless it's able to win or block the opponent's win with a certain move.

## Compilation
- Command-line compile.  In the connectx/ directory run::

		javac -cp ".." *.java */*.java


CXGame application:

- Human vs Computer.  In the connectx/ directory run:
	
		java -cp ".." connectx.CXGame 6 7 4 connectx.Telos.Telos


- Computer vs Computer. In the connectx/ directory run:

		java -cp ".." connectx.CXGame 6 7 4 connectx.Telos.Telos connectx.L1.L1


CXPlayerTester application:

- Output score only:

		java -cp ".." connectx.CXPlayerTester 6 7 4 connectx.Telos.Telos connectx.L1.L1`


- Verbose output

		java -cp ".." connectx.CXPlayerTester 6 7 4 connectx.Telos.Telos connectx.L1.L1 -v


- Verbose output and customized timeout (1 sec) and number of game repetitions (10 rounds)

		java -cp ".." connectx.CXPlayerTester 6 7 4 connectx.Telos.Telos connectx.L1.L1 -v -t 1 -r 10
