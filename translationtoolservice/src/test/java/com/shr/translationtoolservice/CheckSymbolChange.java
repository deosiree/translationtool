package com.shr.translationtoolservice;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName CheckSymbolChange
 * @Description TODO
 * @USER: Cola
 * @Date 2024/7/30 0030 19:27
 **/
public class CheckSymbolChange {
    public static void main(String[] args) {
        Path path1 = Paths.get("D://symbol//old");
        Path path2 = Paths.get("D://symbol//new");

        try {
            compareFolders(path1, path2);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void compareFolders(Path path1, Path path2) throws IOException {
        // 获取两个文件夹的文件列表
        Set<Path> files1 = Files.walk(path1).filter(Files::isRegularFile).collect(Collectors.toSet());
        Set<Path> files2 = Files.walk(path2).filter(Files::isRegularFile).collect(Collectors.toSet());

        // 比较文件列表
        Set<Path> commonFiles = new HashSet<>(files1);
        commonFiles.retainAll(files2);

        // 输出不在两个文件夹中共有的文件
        files1.removeAll(commonFiles);
        files2.removeAll(commonFiles);

        if (!files1.isEmpty()) {
            System.out.println("Folder 1 has extra files: " + files1);
        }
        if (!files2.isEmpty()) {
            System.out.println("Folder 2 has extra files: " + files2);
        }

        // 比较共同文件的内容
        for (Path file : commonFiles) {
            Path file1 = path1.resolve(path1.relativize(file));
            Path file2 = path2.resolve(path2.relativize(file));

            if (!compareFileContent(file1, file2)) {
                System.out.println("Files differ in content: " + file);
            }
        }
    }

    private static boolean compareFileContent(Path file1, Path file2) throws IOException {
        byte[] bytes1 = Files.readAllBytes(file1);
        byte[] bytes2 = Files.readAllBytes(file2);
        return Arrays.equals(bytes1, bytes2);
    }


}
