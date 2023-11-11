package application;

import java.security.SecureRandom;
import java.util.Random;
import java.util.Scanner;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

import globallisteners.GlobalKeyListener;
import util.ANSIUtils;

public class LabGame {

	Random random = new Random(new SecureRandom().nextInt(Integer.MAX_VALUE));
	int mapX, mapY, mapStartX, mapStartY;
	String map[][];
	boolean visited[][];
	String dirs = "awds";
	static String error = "";
	static LabGame game = new LabGame(200, 200, 100, 100);

	public LabGame()
		{ this(3, 3, 1, 1); }

	public LabGame(int mapW, int mapH, int startX, int startY)
		{ resetMap(mapW, mapH, startX, startY); }

	public static void main(String[] args) {
		System.out.println("Maximize a janela de console e pressione enter...");
		try (Scanner sc = new Scanner(System.in))
			{ sc.nextLine(); }
		catch (Exception e) {}
		refresh();
		GlobalKeyListener.startListener();
		GlobalKeyListener.setOnKeyPressedEvent(k -> {
			if (k.getKeyCode() == NativeKeyEvent.VC_R)
				game.resetMap();
			char dir = 0;
			if (k.getKeyCode() == NativeKeyEvent.VC_UP || k.getKeyCode() == NativeKeyEvent.VC_W)
				dir = 'w'; 
			else if (k.getKeyCode() == NativeKeyEvent.VC_DOWN || k.getKeyCode() == NativeKeyEvent.VC_S)
				dir = 's'; 
			else if (k.getKeyCode() == NativeKeyEvent.VC_LEFT || k.getKeyCode() == NativeKeyEvent.VC_A)
				dir = 'a'; 
			else if (k.getKeyCode() == NativeKeyEvent.VC_RIGHT || k.getKeyCode() == NativeKeyEvent.VC_D)
				dir = 'd'; 
			else if (k.getKeyCode() == NativeKeyEvent.VC_ESCAPE) {
				GlobalKeyListener.stopListener(); 
				System.out.println("Game was ended");
				return;
			}
			if (game.isValidDir(dir)) {
				if (!game.isOpenDir(dir))
					error = dir + " -> Blocked direction";
				else
					game.goTo(dir);
			}
			else if (dir != 0)
				error = dir + " -> Invalid direction (Must be in \"asdw\" format (Or use arrow-keys))";
			refresh();
		});
	}
	
	static void printError(String string) {
		ANSIUtils.moveCursorTo(0, 38);
		System.out.print(ANSIUtils.resetFormaters);
		for (int n = 0; n < 130; n++)
			System.out.print(" ");
		ANSIUtils.moveCursorTo(0, 38);
		System.out.print(ANSIUtils.fontColorDarkRed + string + ANSIUtils.resetFormaters);
	}

	static void print(String string) {
		ANSIUtils.moveCursorTo(0, 39);
		System.out.print(ANSIUtils.resetFormaters);
		for (int n = 0; n < 130; n++)
			System.out.print(" ");
		ANSIUtils.moveCursorTo(0, 39);
		System.out.print(string + ANSIUtils.resetFormaters);
	}

	static void refresh() {
		game.drawCurrentRoom();
		print("[ASDW] - Move around\t[R] Reset map\t[ESC] - Exit");
	}

	void resetMap(int mapW, int mapH, int startX, int startY) {
		if (mapW < 2 || mapH < 2)
			throw new RuntimeException("Map size must be at least 2x2");
		if (startX < 0 || startX >= mapW || startY < 0 || startY >= mapH)
			throw new RuntimeException("Invalid start position");
		map = new String[mapH][mapW];
		visited = new boolean[mapH][mapW];
		mapX = mapStartX = startX;
		mapY = mapStartY = startY;
		visited[startY][startX] = true;
		generateCurrentRoom();
	}

	void resetMap() {
		resetMap(map.length, map[0].length, mapStartX, mapStartY);
	}

	String getRoom(int x, int y) {
		if (!isValidMapPosition(x, y))
			return null;
		return map[y][x];
	}

	String getCurrentRoom() {
		if (getRoom(mapX, mapY) == null)
			generateRoom(mapX, mapY);
		return getRoom(mapX, mapY);
	}

	void setRoom(int x, int y, String dirs) {
		if (isValidMapPosition(x, y))
			map[y][x] = dirs;
	}

	void setCurrentRoom(String dirs)
		{ setRoom(mapX, mapY, dirs); }

	Boolean isValidDir(char dir) {
		dir = Character.toLowerCase(dir);
		return (dir == 'a' || dir == 'w' || dir == 'd' || dir == 's');
	}

	Boolean isOpenDir(char dir, int mapX, int mapY) {
		if (!isValidDir(dir))
			throw new RuntimeException(dir + " - Invalid direction (Must be in \"asdw\" format)");
		return map[mapY][mapX].contains("" + dir);
	}

	Boolean isOpenDir(char dir)
		{ return isOpenDir(dir, mapX, mapY); }

	Boolean isOpenDir(String dir, int mapX, int mapY)
		{ return dir != null && isOpenDir(dir.toLowerCase().charAt(0), mapX, mapY); }

	Boolean isValidMapPosition(int x, int y)
		{ return map != null && x >= 0 && x < map[0].length && y >= 0 && y < map.length; }

	void goTo(int mapX, int mapY) {
		if (!isValidMapPosition(mapX, mapY))
			throw new RuntimeException("Invalid map position");
		this.mapX = mapX;
		this.mapY = mapY;
		generateCurrentRoom();
	}

	public void goTo(char dir) {
		dir = Character.toLowerCase(dir);
		if (dir == 'a')
			mapX--;
		else if (dir == 'w')
			mapY--;
		else if (dir == 'd')
			mapX++;
		else if (dir == 's')
			mapY++;
		else
			throw new RuntimeException(dir + " - Invalid direction (Must be in \"asdw\" format)");
		if (isValidMapPosition(mapX, mapY))
			visited[mapY][mapX] = true;
	}

	static String opositeDir(char dir) {
		dir = Character.toLowerCase(dir);
		if (dir == 'a')
			return "d";
		if (dir == 'w')
			return "s";
		if (dir == 'd')
			return "a";
		if (dir == 's')
			return "w";
		return null;
	}

	void generateRoom(int x, int y) {
		setRoom(x, y, "");
		int[][] inc = { { -1, 0, 1, 0 }, { 0, -1, 0, 1 } };
		int free = 4, ok = 0;
		while (getRoom(x, y).length() < (free > 1 ? 2 : 1)) {
			for (int xx, yy, i = 0; i < 4; i++) {
				xx = x + inc[0][i];
				yy = y + inc[1][i];
				if (isValidMapPosition(xx, yy)) {
					if (getRoom(xx, yy) != null) {
						if (!isOpenDir(opositeDir(dirs.charAt(i)), xx, yy)) {
							if (ok == 0)
								free--;
						}
						else if (!isOpenDir(dirs.charAt(i), x, y))
							setRoom(x, y, getRoom(x, y) + dirs.charAt(i));
					}
					else if (random.nextInt(2) == 0 && !getRoom(x, y).contains("" + dirs.charAt(i)))
						setRoom(x, y, getRoom(x, y) + dirs.charAt(i));
				}
				else if (ok == 0)
					free--;
			}
			ok = 1;
		}
	}

	void generateCurrentRoom()
		{ generateRoom(mapX, mapY); }

	Boolean[] getDoors(int mapX, int mapY) {
		if (!isValidMapPosition(mapX, mapY))
			return null;
		return new Boolean[] { getRoom(mapX, mapY).contains("a"), getRoom(mapX, mapY).contains("w"),
														getRoom(mapX, mapY).contains("d"), getRoom(mapX, mapY).contains("s") };
	}

	Boolean[] getCurrentDoors()
		{ return getDoors(mapX, mapY); }

	void drawCurrentRoom() {
		String room = getCurrentRoom(), roomColor = getRoomColor(mapX, mapY);
		int xx = 1, yy = 1, h = 36, w = h * 2;
		for (int y = yy; y < yy + h; y++)
			for (int x = xx; x < xx + w; x += 2) {
				ANSIUtils.moveCursorTo(x, y);
				if ((x <= xx + 1 || x >= xx + w - 2 || y <= yy || y >= yy + h - 1) &&
						(x < xx + w / 2 - 12 || x > xx + w / 2 + 12 || !room.contains(y <= yy ? "w" : "s")) &&
						(y < yy + h / 2 - 6 || y > yy + h / 2 + 6 || !room.contains(x <= xx + 1 ? "a" : "d")))
							System.out.print(roomColor + "  ");				System.out.print(ANSIUtils.resetFormaters + "  ");

			}
		
		for (int y = 0; y < 3; y++)
			for (int x = 0; x < 3; x++)
				drawMiniRoom(mapX + (-1 + x), mapY + (-1 + y), 75 + x * 12, 1 + y * 6);
		
		for (int y = 0; y < 22; y++)
			for (int x = 0; x < 22; x++)
				drawDotRoom(mapX + (-11 + x), mapY + (-11 + y), 113 + x * 2, 1 + y);

		print("");		printError(error);
		error = "";
	}
	
	String getRoomColor(int roomX, int roomY) {
		if (getRoom(roomX, roomY) == null)
			return null;
		return roomX == mapX && roomY == mapY ? ANSIUtils.bgColorLightBlue :
			visited[roomY][roomX] ? ANSIUtils.bgColorDarkGreen : ANSIUtils.bgColorDarkYellow;
	}

	void drawMiniRoom(int roomX, int roomY, int x, int y) {
		String room = getRoom(roomX, roomY), roomColor = getRoomColor(roomX, roomY);
		for (int yy = 0; yy < 6; yy++)
			for (int xx = 0; xx < 12; xx += 2) {
				ANSIUtils.moveCursorTo(x + xx, y + yy);
				if (!isValidMapPosition(roomX, roomY))
					System.out.print(ANSIUtils.bgColorDarkRed + "  ");
				else if (room == null)
					System.out.print(ANSIUtils.bgColorLightBlack + "  ");
				else if ((xx <= 1 || xx >= 10 || yy == 0 || yy == 5) &&
						(xx < 4 || xx > 7 || !room.contains(yy == 0 ? "w" : "s")) &&
						(yy < 2 || yy > 3 || !room.contains(xx <= 1 ? "a" : "d")))
							System.out.print(roomColor + "  ");
				System.out.print(ANSIUtils.resetFormaters + "  ");
			}
	}

	void drawDotRoom(int roomX, int roomY, int x, int y) {
		ANSIUtils.moveCursorTo(x, y);
		System.out.print((!isValidMapPosition(roomX, roomY) ? ANSIUtils.bgColorDarkRed :
			getRoom(roomX, roomY) == null ? ANSIUtils.bgColorLightBlack : getRoomColor(roomX, roomY)) + "  ");
	}

}