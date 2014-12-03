/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package loancreditscore;

/**
 *
 * @author Kasper
 */
public interface ICreditBureauGateway {
    
    public int getCreditScore(String ssn);
}
