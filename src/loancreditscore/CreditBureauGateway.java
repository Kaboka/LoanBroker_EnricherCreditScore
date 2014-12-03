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
public class CreditBureauGateway implements ICreditBureauGateway{

    @Override
    public int getCreditScore(String ssn) {
        loanbroker.client.CreditScoreService_Service service = new loanbroker.client.CreditScoreService_Service();
        loanbroker.client.CreditScoreService port = service.getCreditScoreServicePort();
        return port.creditScore(ssn);
    }
    
}
