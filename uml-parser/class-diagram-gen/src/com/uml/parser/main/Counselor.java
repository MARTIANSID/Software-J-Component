package com.uml.parser.main;

import java.util.ArrayList;
import java.util.List;

import com.uml.parser.enums.Modifiers;
import com.uml.parser.enums.RelationType;
import com.uml.parser.model.Relationship;
import com.uml.parser.model.UMLClass;
import com.uml.parser.model.UMLMethod;
import com.uml.parser.model.UMLVariable;

import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.expr.VariableDeclarationExpr;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.Type;

/**	
 * Deals with creating relationships between {@link UMLClass} by using the 
 * relations defined in {@link RelationType}
 * Maintains data for all the {@link UMLClass} and their {@link Relationship}
 * @author rishi
 *
 */
public class Counselor {
	
	private static Counselor counselor;
	private List<Relationship> relationships;
	private List<UMLClass> umlClasses;
	
	/**
	 * Returns the instance for singleton class
	 * @return
	 */
	public static Counselor getInstance(){
		if(counselor == null){
			counselor = new Counselor();
		}
		return counselor;
	}
	
	/**
	 * Private constructor to implement singleton class
	 */
	private Counselor() {
		relationships = new ArrayList<>();
		umlClasses = new ArrayList<>();
	}

	/**
	 * Check relationship between extended classes or implemented interfaces for given
	 * {@link UMLClass}
	 * 
	 * @param umlClass
	 * @param type
	 */
	public void checkForRelatives(UMLClass umlClass, TypeDeclaration type){
		ClassOrInterfaceDeclaration classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) type;
		if(classOrInterfaceDeclaration.getExtends() != null){
			List<ClassOrInterfaceType> extendList = classOrInterfaceDeclaration.getExtends();
			for(ClassOrInterfaceType ext : extendList){
				umlClass.addParent(ext.getName());
				createRelationship(umlClass, ext, RelationType.GENERALIZATION);
			}
		}
		
		if(classOrInterfaceDeclaration.getImplements() != null){
			List<ClassOrInterfaceType> implementList = classOrInterfaceDeclaration.getImplements();
			for(ClassOrInterfaceType imp : implementList){
				umlClass.addParent(imp.getName());
				createRelationship(umlClass, imp, RelationType.REALIZATION);
			}
		}
	}
	
	/**
	 * Checks for dependency relationship, if object of other class is used in method parameters. 
	 * @param umlClass
	 * @param method
	 */
	public void checkForRelatives(UMLClass umlClass, UMLMethod method){
		if(method.getParameters() != null){
			List<Parameter> parameters = method.getParameters();
			for(Parameter parameter : parameters){
				if(UMLHelper.isUMLClassType(parameter.getType())){
					createRelationship(umlClass, parameter.getType(), RelationType.DEPENDENCY);
				}
			}
		}
	}
	
	/**
	 * Checks relationships for instance variables declared for class.
	 * @param umlClass
	 * @param field
	 */
	public void checkForRelatives(UMLClass umlClass, UMLVariable field){
		if(field.isUMLClassType()){	
			createRelationship(umlClass, field.getType(), RelationType.ASSOCIATION);
		}
	}
	
	/**
	 * Checks for relatives in the method body
	 * @param umlClass
	 * @param variableDeclarationExpr
	 */
	public void checkForRelatives(UMLClass umlClass, VariableDeclarationExpr variableDeclarationExpr){
		Type variableType = variableDeclarationExpr.getType();
		if(UMLHelper.isUMLClassType(variableType)){
			createRelationship(umlClass, variableType, RelationType.DEPENDENCY);
		}
	}
	
	/**
	 * If there is a relationship they are created here. 
	 * @param umlClass
	 * @param relative
	 * @param relationType
	 */
	public void createRelationship(UMLClass umlClass, Type relative, RelationType relationType){
		Relationship relationship = new Relationship();
		relationship.setType(relationType);
		if(UMLHelper.isUMLClassArray(relative)){
			relationship.setParent(counselor.getUMLClass(UMLHelper.getArrayClassName(relative)));
			if(relationType == RelationType.ASSOCIATION){
				relationship.setParentCardinality("1");
				relationship.setChildCardinality("0..*");
			}
		}else {
			relationship.setParent(counselor.getUMLClass(relative.toString()));
		}
		relationship.setChild(umlClass);
		addRelation(relationship);
		
//		// As there is a relationship it means child is also a Class or Interface, so adding it to the list
//		// of UMLClasses.
//		UMLClass childUMLClass = counsDelor.getUMLClass(relationship.getChild());
//		counselor.addUMLClass(childUMLClass);
	}
	
	
	/**
	 * Returns all the relationship
	 * @return the relationships
	 */
	public List<Relationship> getRelationships() {
		return relationships;
	}

	/**
	 * Setter for setting relationships
	 * @param relationships the relationships to set
	 */
	public void setRelationships(List<Relationship> relationships) {
		this.relationships = relationships;
	}

	/**
	 * Adds {@link Relationship} by checking if its already present or not
	 * @param newRelation
	 */
	private void addRelation(Relationship newRelation){
		if(relationships.size() > 0){
			for(Relationship oldRelation : relationships){
				if(oldRelation.getParent().getName().equalsIgnoreCase(newRelation.getParent().getName()) && 
						oldRelation.getChild().getName().equalsIgnoreCase(newRelation.getChild().getName()) 
						&& oldRelation.getType() == newRelation.getType()){
					return;
				}else if(oldRelation.getParent().getName().equalsIgnoreCase(newRelation.getChild().getName()) && 
						oldRelation.getChild().getName().equalsIgnoreCase(newRelation.getParent().getName())
						&& oldRelation.getType() == newRelation.getType()){
					return;
				}
			}
		}
		relationships.add(newRelation);
	}
	
	/**
	 * Adds {@link UMLClass}
	 * @param newUMLClass
	 */
	public void addUMLClass(UMLClass newUMLClass){
		if(!hasUMLClass(newUMLClass)){
			umlClasses.add(newUMLClass);
		}
	}
	
	/**
	 * Returns all the {@link UMLClass}
	 * @return
	 */
	public List<UMLClass> getUMLClasses(){
		return umlClasses;
	}
	
	/**
	 * Returns {@link UMLClass} for given name
	 * @param name
	 * @return
	 */
	public UMLClass getUMLClass(String name){
		for(UMLClass umlClass : umlClasses){
			if(umlClass.getName().equalsIgnoreCase(name)){
				return umlClass;
			}
		}
		UMLClass newUMLClass = new UMLClass();
		newUMLClass.setName(name);
		umlClasses.add(newUMLClass);
		return newUMLClass;
	}
	
	/**
	 * Checks if {@link UMLClass} is already present with {@link Counselor}
	 * @param newUMLClass
	 * @return
	 */
	public boolean hasUMLClass(UMLClass newUMLClass){
		for(UMLClass umlClass : umlClasses){
			if(umlClass.getName().equalsIgnoreCase(newUMLClass.getName())){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * If the variable has getter and setter we update it to public
	 * @param umlClass
	 * @param variableName
	 */
	public void updateVariableToPublic(UMLClass umlClass, String variableName){
		for(UMLVariable variable : umlClass.getUMLVariables()){
			if(variable.getName().equalsIgnoreCase(variableName)){
				variable.setModifier(Modifiers.PUBLIC.modifier);
			}
		}
	}
	
	/**
	 * Removes the getter and setter methods, as we are making the variable public
	 * @param umlClass
	 * @param getterMethod
	 * @param setterMethod
	 */
	public void removeSetterGetterMethod(UMLClass umlClass, UMLMethod getterMethod, UMLMethod setterMethod){
		List<UMLMethod> umlMethods = new ArrayList<>();
		for(UMLMethod umlMethod : umlClass.getUMLMethods()){
			if(!umlMethod.getName().equalsIgnoreCase(getterMethod.getName()) && 
					!umlMethod.getName().equalsIgnoreCase(setterMethod.getName())){
				umlMethods.add(umlMethod);
			}
		}
	}
	
	/**
	 * TODO Confirm if this required or not
	 * Removes the methods which are repeated in the Child classes, as they are already defined in parent classes
	 */
	public void removeUnneccessaryMethods(){
		for(Relationship relationship : relationships){
			if(relationship.getType() == RelationType.GENERALIZATION || relationship.getType() == RelationType.REALIZATION){
				UMLClass parent = relationship.getParent();
				UMLClass child = relationship.getChild();
				List<UMLMethod> newChildMethods = child.getUMLMethods();
				
				for(UMLMethod parentMethod : parent.getUMLMethods()){
					if(newChildMethods.contains(parentMethod)){
						newChildMethods.remove(parentMethod);
					}
				}
				child.setUMLMethods(newChildMethods);
			}
		}
	}
}
