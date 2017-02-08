/* Bank Simulation
 * Author : Pulkit Khemka
 * Number of Customers = 5
 * Number of Bank Tellers  = 2
 * Number of Loan Officers = 1
 */

/* Included Libraries */
import java.util.LinkedList; 
import java.util.Queue;
import java.util.concurrent.Semaphore;


/* Class Customer containing the main function and the Customer thread implementation*/
public class Customer implements Runnable
{
	/* private variables to customer*/
	private int task, i, num;
	
	/*public global Semaphores */
	public static Semaphore max_customers = new Semaphore( 5, true ); /* To limit the number of customer threads being run at a time */
	public static Semaphore queue1notempty = new Semaphore( 0, true ); /* To signal that the queue for bank teller is not empty and bank teller can now remove customer from queue */
	public static Semaphore queue2notempty = new Semaphore( 0, true ); /* To signal that the queue for loan officer is not empty and loan officer can now remove customer from queue. */
	public static Semaphore[] banktellerRequest = new Semaphore[] {new Semaphore(0),   new Semaphore(0)}; /* To request for a withdrawal or deposit from bank teller after a specific bank teller is ready to serve that customer. */
	public static Semaphore[] depositReceipt = new Semaphore[] {new Semaphore(0),   new Semaphore(0)}; /* To signal that deposit is processed by a specific teller.  */
	public static Semaphore[] depositComplete = new Semaphore[] {new Semaphore(0),   new Semaphore(0)}; /* To signal that deposit transaction is completed by a specific teller. */
	public static Semaphore[] withdrawReceipt = new Semaphore[] {new Semaphore(0),   new Semaphore(0)}; /* To signal that withdrawal is processed by a specific teller */
	public static Semaphore[] withdrawalComplete = new Semaphore[] {new Semaphore(0),   new Semaphore(0)}; /* To signal that withdrawal transaction is completed by a specific teller. */
	public static Semaphore loanOfficerRequest = new Semaphore( 0, true ); /* To request for a loan from loan officer after loan officer is ready to serve that customer. */
	public static Semaphore loanOfficerReceipt = new Semaphore( 0, true ); /* To signal that loan is approved is processed by loan officer. */
	public static Semaphore loanTransactionComplete = new Semaphore( 0, true ); /* To signal that loan transaction is completed by loan officer. */
	public static Semaphore[] tellerReady = new Semaphore[]{ new Semaphore(0),   new Semaphore(0), new Semaphore(0), new Semaphore(0),new Semaphore(0)}; /* To signal that a teller is ready to serve a specific customer. For each customer we have a semaphore. */
	public static Semaphore[] loanOfficerReady = new Semaphore[]{ new Semaphore(0),   new Semaphore(0), new Semaphore(0), new Semaphore(0),new Semaphore(0)}; /* To signal that a loan officer is ready to serve a specific customer. For each customer we have a semaphore. */
	public static Semaphore mutex1= new Semaphore( 1, true ); /* To ensure mutual exclusion for bank teller queue, so that addition or removal from the queue is done one at a time */
	public static Semaphore mutex2= new Semaphore( 1, true ); /* To ensure mutual exclusion for loan officer queue, so that addition or removal from the queue is done one at a time */

	/* Global Array Variables*/
	public static int[] taskCustomer = new int[10]; /* Task Allotted to a Customer*/
	public static int[] tellerServingCustomer = new int[10]; /* Teller number Serving the Customer */
	public static int[] deposit = new int[10]; /* Amount of deposit for a particular Customer */
	public static int[] withdraw = new int[10]; /* Withdrawal amount for a particular Customer */
	public static int[] balance = new int[10]; /* Balance of each Customer */
	public static int[] loan = new int[10]; /* Amount of Loan for a particular customer for a transaction*/
	public static int[] loanTotal = new int[10]; /* Total Amount of Loan Taken by a Customer */
	
	/* Global Queues*/
	public static Queue<Integer> queueBankTeller = new LinkedList<Integer>(); /* Queue for Bank Teller */
	public static Queue<Integer> queueLoanOfficer = new LinkedList<Integer>(); /* Queue for loan Officer */
	
	
	
   Customer( int num)
   {
	   /* Setting up of Starting values for each customer instance */
      this.num = num;
      balance[num]=1000;
      loanTotal[num]=0;
   }

   /* Run function for the Customer Thread */
   public void run()  
   {
	   for(i=0;i<3;i++) /* Makes each customer thread run for three times  */
	   {
		   try 
		   {
			max_customers.acquire(); /* Limits the max number of customer threads running at a time */
			task = assigntask();	/* Assigns task deposit, withdraw or loan. */
			taskCustomer[num] = task;
			/* If the task is the deposit task*/
			if(task ==1) 
			{
				mutex1.acquire(); /* critical section for adding to teller queue */
				queueBankTeller.add(num);			
				queue1notempty.release(); /* signals teller that queue is not empty */
				mutex1.release();			
				tellerReady[num].acquire(); /* waits till teller is ready for this customer thread */
				deposit[num] = 100 * (1 + (int)(Math.random()*5));
				
				System.out.println("Customer "+ num + " requests of teller " + tellerServingCustomer[num] + " to make a deposit of $" + deposit[num]);
				Thread.sleep(100);
				banktellerRequest[tellerServingCustomer[num]].release(); /* signals teller to start processing deposit */
				
				depositReceipt[tellerServingCustomer[num]].acquire();  /* waits till teller is done with processing */
				Thread.sleep(100);
				System.out.println("Customer "+ num + " gets receipt from teller " + tellerServingCustomer[num]);
				depositComplete[tellerServingCustomer[num]].release();	/* signals teller to move to next customer */
			}
			/* If task is the withdrawal task */
			if(task ==2) /* works similar to deposit task */
			{
				mutex1.acquire();
				queueBankTeller.add(num);			
				queue1notempty.release();
				mutex1.release();
				tellerReady[num].acquire();
				withdraw[num] = 100 * (1 + (int)(Math.random()*5));
				
				System.out.println("Customer "+ num + " requests of teller " + tellerServingCustomer[num] + " to make a withdrawal of $" + withdraw[num]);
				Thread.sleep(100);
				banktellerRequest[tellerServingCustomer[num]].release();
				
				withdrawReceipt[tellerServingCustomer[num]].acquire();	
				Thread.sleep(100);
				System.out.println("Customer "+ num + " gets cash and receipt from teller " + tellerServingCustomer[num]);
				withdrawalComplete[tellerServingCustomer[num]].release();
			}
			/* If task is the loan task */
			if(task ==3) 
			{
				mutex2.acquire(); /* critical section for adding to loan officer queue */
				queueLoanOfficer.add(num);			
				queue2notempty.release(); /* signals loan officer that queue is not empty */
				mutex2.release();
				loanOfficerReady[num].acquire(); /* waits till officer is ready for this thread */
				loan[num] = 100 * (1 + (int)(Math.random()*5));
				
				System.out.println("Customer "+ num + " requests of Loan Officer to apply for a loan of $" + loan[num]);
				Thread.sleep(100);
				loanOfficerRequest.release(); /* signals teller to start processing loan */
				
				loanOfficerReceipt.acquire();  /* waits till loan officer is done with processing */
				Thread.sleep(100);
				System.out.println("Customer "+ num + " gets loan from Loan Officer");
				loanTransactionComplete.release(); /* signals officer to move to next customer */
			}
			max_customers.release();
		   }
		   catch (InterruptedException e) 
		   {
			e.printStackTrace();
		   }
	   }
	   
	   System.out.println("Customer "+ num +" departs the bank");
   }

   /* Function to Assign Task Randomly */
   private int assigntask() 
   {
	   int randNum;
	   randNum = 1 + (int)(Math.random()*3);
	   return randNum;
   }

   public static void main(String args[])
   {
	  int i= 0;    
	  final int NUMCUSTOMERS = 5;
	  int sumBalance =0, sumLoanTotal =0;
	  
	  /* Thread created for Loan Officer */	  
	  LoanOfficer officer = new LoanOfficer();	  
	  Thread myThread2 = new Thread();
	  myThread2 = new Thread(officer);
	  myThread2.setDaemon(true);
	  myThread2.start();
	  System.out.println("Loan Officer created ");	
	  
	  /* Threads created for Bank Tellers */
	  Bankteller teller[] = new Bankteller[2];
	  Thread myThread1[] = new Thread[2];
	  for( i = 0; i < 2; ++i ) 
      {
    	 teller[i] = new Bankteller(i);
         myThread1[i] = new Thread( teller[i] );
         myThread1[i].setDaemon(true);
         myThread1[i].start();
         System.out.println("Teller "+i+" created ");
      } 
	  
	  /* Threads created for Customers */
	  Customer cust[] =  new Customer[NUMCUSTOMERS];
      Thread myThread[] = new Thread[NUMCUSTOMERS];
      for( i = 0; i < NUMCUSTOMERS; ++i ) 
      {
    	 cust[i] = new Customer(i);
         myThread[i] = new Thread( cust[i] );
         myThread[i].start();
         System.out.println("Customer "+i+" created ");
      }
      
      
      /* Customer Threads Joined */
      for( i = 0; i < NUMCUSTOMERS; ++i ) 
      {
    	  try
    	  {
    		  myThread[i].join();
    		  System.out.println("Customer "+i+" joined by main");
    	  }
    	  catch (InterruptedException e)
    	  {
    	  }
      }
      
      
      /* Printing the Bank Simulation Summary */
      System.out.println("\n\t   Bank Simulation Summary\n");
      System.out.println("\t\tEnding Balance \tLoan Amount\n");
      for(i=0;i<5;i++)
      {
     	System.out.println("Customer "+i+"\t"+balance[i]+"\t\t"+loanTotal[i]); 
     	sumBalance = sumBalance + balance[i];
     	sumLoanTotal = sumLoanTotal + loanTotal[i];
      }      
      System.out.println("\nTotals\t\t"+sumBalance+"\t\t"+ sumLoanTotal);
      
   }
}

/* Class Bankteller responsible for the bank teller thread implementation */
class Bankteller implements Runnable
{
	/* Private Variables */
	private int nextcustomer;
	private int nextcustomertask;
	private int num;
		
	Bankteller(int num)
	{
		 /* Setting up of Starting values for each Bankteller instance */
	    this.num = num;
	}
	
	/* run function for Bankteller thread */
	public void run()
	{
		while(true)
		{
			
			try 
			{
					Customer.queue1notempty.acquire(); /* wait till bank teller queue is not empty */
					Customer.mutex1.acquire();	 /* critical section for removing from teller queue */		
					nextcustomer = Customer.queueBankTeller.remove();
					Customer.mutex1.release();					
					nextcustomertask = Customer.taskCustomer[nextcustomer];						
					Customer.tellerServingCustomer[nextcustomer] = num;
					System.out.println( "Teller " + num + " Begins serving Customer "+ nextcustomer);
					Customer.tellerReady[nextcustomer].release(); /* teller signals customer that it is ready to serve */
					/* If customer task is deposit */
					if(nextcustomertask ==1)
					{
						Customer.banktellerRequest[num].acquire(); /* waits for customers request for processing */
						System.out.println( "Teller " + num + " processes deposit for Customer "+ nextcustomer);
						Thread.sleep(400);
						Customer.balance[nextcustomer] = Customer.balance[nextcustomer] + Customer.deposit[nextcustomer];
						Customer.depositReceipt[num].release(); /* signals that processing is done */
						Customer.depositComplete[num].acquire(); /* wait till customer frees the teller */
					}
					/* If customer task is withdrawal */
					if(nextcustomertask ==2) /* Works Similar to Deposit */
					{						
						Customer.banktellerRequest[num].acquire();
						System.out.println( "Teller " + num + " processes withdrawal for Customer "+ nextcustomer);
						Thread.sleep(400);
						Customer.balance[nextcustomer] = Customer.balance[nextcustomer] - Customer.withdraw[nextcustomer];
						Customer.withdrawReceipt[num].release();
						Customer.withdrawalComplete[num].acquire();
					}
						
			} 
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}			
		}
	}
}

/* Class responsible for implementation of Loan Officer Thread */
class LoanOfficer implements Runnable
{
	private int nextcustomer;
	private int nextcustomertask;

	@Override
	public void run() 
	{
		while(true)
		{
			
			try 
			{
					Customer.queue2notempty.acquire(); /* wait till loan officer queue is not empty */
					Customer.mutex2.acquire();	/* critical section for removing from loan officer queue */				
					nextcustomer = Customer.queueLoanOfficer.remove();
					Customer.mutex2.release();					
					nextcustomertask = Customer.taskCustomer[nextcustomer];						
					System.out.println( "Loan Officer Begins serving Customer "+ nextcustomer);
					Customer.loanOfficerReady[nextcustomer].release(); /* officer signals customer that it is ready to serve */
					if(nextcustomertask ==3)
					{
						Customer.loanOfficerRequest.acquire(); /* waits for customers request for processing */
						System.out.println( "Loan Officer approves loan for Customer "+ nextcustomer);
						Thread.sleep(400);
						Customer.loanTotal[nextcustomer] = Customer.loanTotal[nextcustomer] + Customer.loan[nextcustomer];
						Customer.loanOfficerReceipt.release(); /* signals that processing is done */
						Customer.loanTransactionComplete.acquire(); /* wait till customer frees the loan officer */
					}							
			} 
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}			
		}
		
		
	}
}


