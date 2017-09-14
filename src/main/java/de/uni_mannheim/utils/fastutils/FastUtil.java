package de.uni_mannheim.utils.fastutils;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.booleans.BooleanArrayList;

/**
 * @author Kiril Gashteovski
 */
public class FastUtil {
    /**
     * Given an IntArrayList object, sort it, and return an integer array, containing the sorted elements
     * @param intList: the input list to be sorted
     * @return a sorted integer array
     */
    public static int [] sort(IntArrayList intList){
        // Sort the indices and return them
        int [] sorted = new int[intList.size()];
        for (int i = 0; i < intList.size(); i++){
            sorted[i] = intList.getInt(i);
        }
        IntArrays.quickSort(sorted);
        
        return sorted;
    }
    
    /**
     * Given a number of elements 'n', create a boolean array list of size n, where each element is set to 'false'
     * by default.
     * @param n: size of the list
     * @return a BooleanArrayList of n elements, all of them set to 'false'
     */
    public static BooleanArrayList createFalseBooleanArrayList(int n){
        BooleanArrayList bal = new BooleanArrayList(n);
        for (int i = 0; i < n; i++){
            bal.set(i, false);
        }
        return bal;
    }
    
    /**
     * Given an integer list and an integer 'element', count how many times this element occurs in the list
     * @param element: the element to be counted from the list
     * @param list: the list from which the element is being counted from
     * @return the number of occurrences of the element in the list
     */
    public static int countElement(int element, IntArrayList list){
        int count = 0;
        for (int x: list){
            if (x == element)
                count++;
        }
        return count;
    }
    
    /**
     * Given a list of lists, and a list of lists of integers, which is a combination of indices between the elements of 
     * "lists", get the set of all elements' combinations. For example, if we have a list of the list 'combinationsInd' which is
     * [1, 2], and the list of lists 'lists' is [[1, 2, 3], [4, 5], [6, 7, 8]], then this function will add the following lists 
     * to the result: [[4, 6], [4, 7], [4, 8], [5, 7], [5, 8]] 
     * @param combinationsInd: list of indices of the lists to be combined
     * @param lists: list of lists
     * @return
     */
    public static <T> ObjectOpenHashSet<ObjectArrayList<T>> getListsElementsCombinationSet(
                            ObjectArrayList<IntArrayList> combinationsInd, ObjectArrayList<ObjectArrayList<T>> lists){
        ObjectOpenHashSet<ObjectArrayList<T>> combinationSets = new ObjectOpenHashSet<>();
        ObjectArrayList<ObjectArrayList<T>> tempLists = new ObjectArrayList<>();

        for (IntArrayList indList: combinationsInd){
            tempLists.clear();
            for (int index: indList){
                tempLists.add(lists.get(index));
            }
            combinationSets.addAll(getElementsCombinations(tempLists));
        }
        return combinationSets;
    }
    
    /**
     * Given a list of lists, return all the combinations between the lists (i.e. their indices). For example, suppose we
     * have the list of lists: [[1, 2, 3], [4, 5], [6, 7, 8]]. Then, this function will return:
     * [[0, 1], [1, 0], [0, 2], [2, 0], [1, 2], [2, 1], 
     *  [0, 1, 2], [0, 2, 1], [1, 0, 2], [1, 2, 0], [2, 1, 0], [2, 0, 1]]
     * @param lists: list of lists
     * @return
     */
    public static <T> ObjectArrayList<IntArrayList> getListsCombinationIndices(ObjectArrayList<ObjectArrayList<T>> lists){
        ObjectArrayList<IntArrayList> combinationsInd = new ObjectArrayList<>();
        ObjectArrayList<IntArrayList> result = new ObjectArrayList<>();
        int[][] combinations;
        
        for (int k = 2; k <= lists.size(); k++){
            result.clear();
            combinations = null;
            
            combinations = getCombinations(k, lists.size());
            
            for (int i = 0; i < combinations.length; i++) {
                IntArrayList indices = new IntArrayList();
                for (int j = 0; j < combinations[i].length; j++) {
                    indices.add(combinations[i][j]);
                }
                permute(indices, 0, result);
            }
            combinationsInd.addAll(result);
        }
        return combinationsInd;
    }
    
    /**
     * Given a list of lists, get the combinations of the elements between the lists.
     * For example, if we have lists = [[1, 2, 3], [4, 5]], then 
     * getElementsCombinations(lists) = [1, 4], [1, 5], [2, 4], [2, 5], [3, 4], [3, 5] 
     * @param lists: list of lists
     * @return combination of elements between the lists
     */
    public static <T> Set<ObjectArrayList<T>> getElementsCombinations(ObjectArrayList<ObjectArrayList<T>> lists) {
        Set<ObjectArrayList<T>> combinations = new HashSet<ObjectArrayList<T>>();
        Set<ObjectArrayList<T>> newCombinations = new HashSet<ObjectArrayList<T>>();
        ObjectArrayList<T> newList = new ObjectArrayList<T>();
        
        int index = 0;

        // Extract each of the integers in the first list and add each to ints as a new list
        for(T i: lists.get(0)) {
            newList.clear();
            newList.add(i);
            combinations.add(newList.clone());
        }
        index++;
        List<T> nextList;
        while(index < lists.size()) {
            nextList = lists.get(index).clone();
            newCombinations.clear();
            for(List<T> first: combinations) {
                for(T second: nextList) {
                    newList.clear();
                    newList.addAll(first);
                    newList.add(second);
                    newCombinations.add(newList.clone());
                }
            }
            combinations = newCombinations;

            index++;
            nextList.clear();
        }

        return combinations;
    }
    
    /**
     * Given an integers 'k' and 'n', generate all possible combinations from with length 'k' out of 'n'.
     * For example, if n = 5, and k = 3, then the function will return the array:
     * [[0, 1, 2], [0, 1, 3], [0, 1, 4], [0, 2, 3], [0, 2, 4], [0, 3, 4], [1, 2, 3], [1, 2, 4], [1, 3, 4], [2, 3, 4]] 
     * @param k: length of each combination
     * @param n: length of the list
     * @return: array of integers with all combinations of size 'k' out of 'n'
     */
    private static int[][] getCombinations(int k, int n) {
        int possibilities = get_nCr(n, k);
        int[][] combinations = new int[possibilities][k];
        int arrayPointer = 0;

        int[] counter = new int[k];

        for (int i = 0; i < k; i++) {
            counter[i] = i;
        }
        breakLoop: while (true) {
            // Initializing part
            for (int i = 1; i < k; i++) {
                if (counter[i] >= n - (k - 1 - i)) {
                    counter[i] = counter[i - 1] + 1;
                }
            }

            // Testing part
            for (int i = 0; i < k; i++) {
                if (counter[i] < n) {
                    continue;
                } else {
                    break breakLoop;
                }
            }

            // Innermost part
            combinations[arrayPointer] = counter.clone();
            arrayPointer++;

            // Incrementing part
            counter[k - 1]++;
            for (int i = k - 1; i >= 1; i--) {
                if (counter[i] >= n - (k - 1 - i)){
                    counter[i - 1]++;
                }
            }
        }

        return combinations;
    }

    /**
     * Get the number of combinations. 
     * For example, if n = 9 and r = 4, then get_nCr(9, 4) = 126
     * @param n: integer
     * @param r: integer, r < n
     * @return
     */
    private static int get_nCr(int n, int r) {
        int numerator = 1;
        int denominator = 1;
        for (int i = n; i >= r + 1; i--) {
            numerator *= i;
        }
        for (int i = 2; i <= n - r; i++) {
            denominator *= i;
        }

        return (int) (numerator / denominator);
    }
    
    /**
     * Given a list of integers 'intList', make all the permutations of the elements in the list. The result is stored
     * in a list of list of integers 'result'. The parameter 'k' should always be set to 0 (it is used for the purposes of 
     * the recursion). 
     * For example, if intList = [0, 1, 2], then the result would be:
     * result = [[0, 1, 2], [0, 2, 1], [1, 0, 2], [1, 2, 0], [2, 1, 0], [2, 0, 1]]
     * @param intList: list of integers
     * @param k: 
     * @param result: permutations of the integer list (list of lists)
     */
    private static void permute(IntArrayList intList, int k, ObjectArrayList<IntArrayList> result){
        // Avoid waaay to many permutations
        if (k > 1000) {
            return;
        }
        
        for(int i = k; i < intList.size(); i++){
            java.util.Collections.swap(intList, i, k);
            permute(intList, k + 1, result);
            java.util.Collections.swap(intList, k, i);
        }
        if (k == intList.size() -1){
            result.add(intList.clone());
        }
    }
    
    /**
     * Given a list of strings, return one string representing them, separated by 'separator
     * @param stList: list of strings
     * @param separator: separator for the strings
     */
    public static String listOfStringsToString(ObjectArrayList<String> stList, String separator){
        StringBuffer sb = new StringBuffer();
        for (String st: stList){
            sb.append(st);
            sb.append(separator);
        }
        return sb.toString().trim();
    }
}
