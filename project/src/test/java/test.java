import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

public class test {
    public static void main(String[] args) {
        char[][] DingMingMing = new char[9][9];
        for (int i = 0; i < DingMingMing.length; i++) {
            Arrays.fill(DingMingMing[i],'.');
        }
        DingMingMing[0][0] = '8';
        DingMingMing[1][2] = '3';
        DingMingMing[1][3] = '6';
        DingMingMing[2][1] = '7';
        DingMingMing[2][4] = '9';
        DingMingMing[2][6] = '2';
        DingMingMing[3][1] = '5';
        DingMingMing[3][5] = '7';
        DingMingMing[4][4] = '4';
        DingMingMing[4][5] = '5';
        DingMingMing[4][6] = '7';
        DingMingMing[5][3] = '1';
        DingMingMing[5][7] = '3';
        DingMingMing[6][2] = '1';
        DingMingMing[6][7] = '6';
        DingMingMing[6][8] = '8';
        DingMingMing[7][2] = '8';
        DingMingMing[7][3] = '5';
        DingMingMing[7][7] = '1';
        DingMingMing[8][1] = '9';
        DingMingMing[8][6] = '4';
        new test().solveSudoku(DingMingMing);






        System.out.println(LocalDate.now(ZoneId.of("Asia/Shanghai")));
//        BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);
//
//        // 生产者线程
//        new Thread(() -> {
//            try {
//                queue.put("Element 1");
//                Thread.sleep(1000); // 模拟生产时间
//                queue.put("Element 2");
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }).start();
//
//        // 消费者线程
//        new Thread(() -> {
//            try {
//                System.out.println(queue.take());
//                System.out.println(queue.take());
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }).start();
        //int 绝对值排序 正反序
        int[] a = {1,-100,90,4,2,-10000};
        String[] str = { "qw","bc","ddd"};
//        int[] ints = Arrays.stream(a).boxed().sorted((m, n) -> Integer.compare(Math.abs(m), Math.abs(n))).mapToInt(Integer::intValue).toArray();
//        int[] ints = Arrays.stream(a).boxed().sorted((m, n) -> Integer.compare(Math.abs(m), Math.abs(n))).mapToInt(x -> x*x).toArray();
//        System.out.println(Arrays.toString(ints));
//        Integer[] in = (Integer[]) Arrays.stream(a).boxed().toArray();
//        Optional<String> a1 = Arrays.stream(str).filter(s -> s.startsWith("a")).findFirst();
//
//        a1.ifPresent(System.out::println);
//        System.out.println(a1);
        List<String> collect = Arrays.stream(str).toList();
        System.out.println(collect);
        String[] strings = collect.toArray(new String[0]);
        System.out.println(Arrays.toString(strings));
        List<Integer> integers = Arrays.stream(a).boxed().toList();
        System.out.println(integers);
        int[] ints = integers.stream().mapToInt(Integer::intValue).toArray();
        System.out.println(Arrays.toString(ints));
    }
    public void solveSudoku(char[][] board) {
        boolean b = solveSudokuHelper(board);
        if(b){
            for (int i = 0; i < board.length; i++) {
                System.out.println(Arrays.toString(board[i]));
            }
        }
    }
    private boolean solveSudokuHelper(char[][] board){
        //「一个for循环遍历棋盘的行，一个for循环遍历棋盘的列，
        // 一行一列确定下来之后，递归遍历这个位置放9个数字的可能性！」
        for (int i = 0; i < 9; i++){ // 遍历行
            for (int j = 0; j < 9; j++){ // 遍历列
                if (board[i][j] != '.'){ // 跳过原始数字
                    continue;
                }
                for (char k = '1'; k <= '9'; k++){ // (i, j) 这个位置放k是否合适
                    if (isValidSudoku(i, j, k, board)){
                        board[i][j] = k;
                        if (solveSudokuHelper(board)){ // 如果找到合适一组立刻返回
                            return true;
                        }
                        board[i][j] = '.';
                    }
                }
                // 9个数都试完了，都不行，那么就返回false
                return false;
                // 因为如果一行一列确定下来了，这里尝试了9个数都不行，说明这个棋盘找不到解决数独问题的解！
                // 那么会直接返回， 「这也就是为什么没有终止条件也不会永远填不满棋盘而无限递归下去！」
            }
        }
        // 遍历完没有返回false，说明找到了合适棋盘位置了
        return true;
    }

    /**
     * 判断棋盘是否合法有如下三个维度:
     *     同行是否重复
     *     同列是否重复
     *     9宫格里是否重复
     */
    private boolean isValidSudoku(int row, int col, char val, char[][] board){
        // 同行是否重复
        for (int i = 0; i < 9; i++){
            if (board[row][i] == val){
                return false;
            }
        }
        // 同列是否重复
        for (int j = 0; j < 9; j++){
            if (board[j][col] == val){
                return false;
            }
        }
        // 9宫格里是否重复
        int startRow = (row / 3) * 3;
        int startCol = (col / 3) * 3;
        for (int i = startRow; i < startRow + 3; i++){
            for (int j = startCol; j < startCol + 3; j++){
                if (board[i][j] == val){
                    return false;
                }
            }
        }
        return true;
    }
}
