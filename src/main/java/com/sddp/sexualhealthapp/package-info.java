/**
 * Root package for the Sexual Health App.
 *
 * <h2>Architecture Overview</h2>
 * This application uses a strict MVC (Model-View-Controller) architecture:
 * <ul>
 *   <li><b>calculator</b> - Calculator disguise and authentication system</li>
 *   <li><b>mainapp</b> - Main sexual health app features (articles, events, etc.)</li>
 *   <li><b>security</b> - Security and encryption utilities</li>
 *   <li><b>navigation</b> - Scene management and transitions</li>
 *   <li><b>util</b> - Application-wide utilities and constants</li>
 * </ul>
 *
 * <h2>For New Team Members</h2>
 * If you're adding a new feature:
 * <ol>
 *   <li>Create your classes in the <code>mainapp</code> package</li>
 *   <li>Follow the MVC pattern (see existing examples)</li>
 *   <li>Use SceneManager for navigation</li>
 *   <li>Store constants in AppConstants</li>
 * </ol>
 *
 * @see com.sddp.sexualhealthapp.calculator Calculator disguise system
 * @see com.sddp.sexualhealthapp.mainapp Main app features
 * @see com.sddp.sexualhealthapp.navigation.SceneManager Scene navigation
 *
 * @author SDDP Group 30
 * @version 1.0
 */
package com.sddp.sexualhealthapp;
