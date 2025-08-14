package com.microsoft.azure.servicebus.jms;

import org.junit.Test;

import javax.jms.JMSException;
import javax.jms.Queue;

import static org.junit.Assert.*;

public class ServiceBusJmsQueueTest {
    
    @Test
    public void testConstructorWithValidName() throws JMSException {
        String queueName = "test-queue";
        ServiceBusJmsQueue queue = new ServiceBusJmsQueue(queueName);
        
        assertNotNull(queue);
        assertEquals(queueName, queue.getQueueName());
    }
    
    @Test
    public void testConstructorWithNameTrimming() throws JMSException {
        String queueName = "  test-queue  ";
        ServiceBusJmsQueue queue = new ServiceBusJmsQueue(queueName);
        
        assertEquals("test-queue", queue.getQueueName());
    }
    
    @Test(expected = JMSException.class)
    public void testConstructorWithNullName() throws JMSException {
        new ServiceBusJmsQueue(null);
    }
    
    @Test(expected = JMSException.class)
    public void testConstructorWithEmptyName() throws JMSException {
        new ServiceBusJmsQueue("");
    }
    
    @Test(expected = JMSException.class)
    public void testConstructorWithWhitespaceOnlyName() throws JMSException {
        new ServiceBusJmsQueue("   ");
    }
    
    @Test
    public void testToString() throws JMSException {
        String queueName = "test-queue";
        ServiceBusJmsQueue queue = new ServiceBusJmsQueue(queueName);
        
        String toString = queue.toString();
        assertNotNull(toString);
        assertTrue(toString.contains(queueName));
        assertEquals("ServiceBusQueue[test-queue]", toString);
    }
    
    @Test
    public void testEquals() throws JMSException {
        String queueName = "test-queue";
        ServiceBusJmsQueue queue1 = new ServiceBusJmsQueue(queueName);
        ServiceBusJmsQueue queue2 = new ServiceBusJmsQueue(queueName);
        ServiceBusJmsQueue queue3 = new ServiceBusJmsQueue("different-queue");
        
        // Same queue names should be equal
        assertEquals(queue1, queue2);
        assertEquals(queue2, queue1);
        
        // Different queue names should not be equal
        assertNotEquals(queue1, queue3);
        assertNotEquals(queue3, queue1);
        
        // Same object should be equal
        assertEquals(queue1, queue1);
        
        // Null should not be equal
        assertNotEquals(queue1, null);
        
        // Different type should not be equal
        assertNotEquals(queue1, "not-a-queue");
    }
    
    @Test
    public void testHashCode() throws JMSException {
        String queueName = "test-queue";
        ServiceBusJmsQueue queue1 = new ServiceBusJmsQueue(queueName);
        ServiceBusJmsQueue queue2 = new ServiceBusJmsQueue(queueName);
        
        // Same queue names should have same hash code
        assertEquals(queue1.hashCode(), queue2.hashCode());
        
        // Hash code should be consistent
        int hashCode1 = queue1.hashCode();
        int hashCode2 = queue1.hashCode();
        assertEquals(hashCode1, hashCode2);
    }
    
    @Test
    public void testEqualsWithGenericQueue() throws JMSException {
        String queueName = "test-queue";
        ServiceBusJmsQueue serviceBusQueue = new ServiceBusJmsQueue(queueName);
        
        // Create a mock Queue implementation
        Queue mockQueue = new Queue() {
            @Override
            public String getQueueName() throws JMSException {
                return queueName;
            }
        };
        
        // Should be equal to any Queue with the same name
        assertEquals(serviceBusQueue, mockQueue);
    }
    
    @Test
    public void testEqualsWithQueueThrowingException() throws JMSException {
        String queueName = "test-queue";
        ServiceBusJmsQueue serviceBusQueue = new ServiceBusJmsQueue(queueName);
        
        // Create a mock Queue that throws exception
        Queue exceptionQueue = new Queue() {
            @Override
            public String getQueueName() throws JMSException {
                throw new JMSException("Test exception");
            }
        };
        
        // Should not be equal if the other queue throws exception
        assertNotEquals(serviceBusQueue, exceptionQueue);
    }
}