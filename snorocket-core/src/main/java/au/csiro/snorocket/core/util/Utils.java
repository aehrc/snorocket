/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.core.util;

import java.util.Map;

import au.csiro.ontology.Node;

/**
 * @author Alejandro Metke
 *
 */
public class Utils {
    
    public static void printTaxonomy(Node<String> top, Node<String> bottom, Map<String, String> idNameMap) {
        for(Node<String> child : top.getChildren()) {
            printTaxonomyLevel(child, bottom, 0, idNameMap);
        }
    }
    
    private static void printTaxonomyLevel(Node<String> root, 
            Node<String> bottom, int level, Map<String, String> idNameMap) {
        if(root.equals(bottom)) return;
        System.out.println(spaces(level)+nodeToString(root, idNameMap));
        for(Node<String> child : root.getChildren()) {
            printTaxonomyLevel(child, bottom, level+1, idNameMap);
        }
    }
    
    private static String nodeToString(Node<String> node, Map<String, String> idNameMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for(String concept : node.getEquivalentConcepts()) {
            sb.append(" ");
            sb.append(idNameMap.get(concept));
        }
        sb.append(" }");
        return sb.toString();
    }
    
    public static void printTaxonomy(Node<String> top, Node<String> bottom) {
        for(Node<String> child : top.getChildren()) {
            printTaxonomyLevel(child, bottom, 0);
        }
    }
    
    private static void printTaxonomyLevel(Node<String> root, 
            Node<String> bottom, int level) {
        if(root.equals(bottom)) return;
        System.out.println(spaces(level)+root.toString());
        for(Node<String> child : root.getChildren()) {
            printTaxonomyLevel(child, bottom, level+1);
        }
    }
    
    private static String spaces(int num) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < num; i++) {
            sb.append("  ");
        }
        return sb.toString();
    }

}
