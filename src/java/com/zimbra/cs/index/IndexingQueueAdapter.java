/**
 *
 */
package com.zimbra.cs.index;

import com.zimbra.common.service.ServiceException;

/**
 * @author Greg Solovyev Describes access to the indexing queue
 */
public interface IndexingQueueAdapter {

    /**
     * Add an item to the tail of the queue. If the underlying queue is full
     * this call blocks until there is space in the queue.
     * 
     * @param {@link com.zimbra.cs.index.AbstractIndexingTasksLocator} item
     */
    public boolean put(AbstractIndexingTasksLocator item);

    /**
     * Return the next element from the queue and remove it from the queue.
     * If no element is available, return null.
     * 
     * @return {@link com.zimbra.cs.index.AbstractIndexingTasksLocator}
     */
    public AbstractIndexingTasksLocator take() ;

    /**
     * Return the next element in the queue and keep it in the queue. Returns
     * null if the queue is empty.
     * 
     * @return {@link com.zimbra.cs.index.AbstractIndexingTasksLocator}
     */
    public AbstractIndexingTasksLocator peek();

    /**
     * 
     * @return true if there are more items in the queue, false otherwise
     */
    public boolean hasMoreItems();

    /**
     * drain the queue
     */
    public void drain();

    /**
     * Increment the number of items re-indexed for a specified mailbox. Place a
     * mailboxId - > numTasks record into an underlying KV store Used by
     * re-indexing process to report and monitor progress of re-indexing a
     * mailbox.
     * 
     * @param accountId
     * @param numItems
     */
    public void incrementSucceededMailboxTaskCount(String accountId, int numItems);

    /**
     * Increment the number of items that failed re-indexing
     */
    public void incrementFailedMailboxTaskCount(String accountId, int numItems);

    /**
     * Retrieve the number of items re-indexed for a specified mailbox. Fetch an
     * integer that corresponds to mailboxId from an underlying KV store. Used
     * by re-indexing process to monitor progress of re-indexing a mailbox.
     * 
     * @param accountId
     * @return the number of tasks. Returns 0 if a record corresponding to this
     *         given mailboxId does not exist.
     */
    public int getSucceededMailboxTaskCount(String accountId);

    /**
     * Retrieve the number of items that failed re-indexing
     */
    public int getFailedMailboxTaskCount(String accountId);

    /**
     * Remove the records of how many items were queued for re-indexing and how
     * many items were re-indexed for a specified mailbox. Used by re-indexing
     * process to reset the progress counter.
     * 
     * @param accountId
     */
    public void deleteMailboxTaskCounts(String accountId);

    /**
     * delete all records of counters of items queued for indexing
     */
    public void clearAllTaskCounts();

    /**
     * set the total task count for a given mailbox
     * 
     * @param accountId
     * @param val
     */
    public void setTotalMailboxTaskCount(String accountId, int val);

    /**
     * set number of completed tasks for a given mailbox
     * 
     * @param accountId
     * @param val
     */
    public void setSucceededMailboxTaskCount(String accountId, int val);

    /**
     * set number of failed tasks for a given mailbox
     * 
     * @param accountId
     * @param val
     */
    public void setFailedMailboxTaskCount(String accountId, int i);

    /**
     * get the total task count for a given mailbox
     */
    public int getTotalMailboxTaskCount(String accountId);

    /**
     * Add an item to the tail of the queue. If the underlying queue is full
     * this call returns FALSE;
     * 
     * @param {@link com.zimbra.cs.index.AbstractIndexingTasksLocator} item
     * @return boolean true if the item was successfully added/false otherwise
     * @throws ServiceException
     */
    public boolean add(AbstractIndexingTasksLocator item) throws ServiceException;

    /**
     * 
     * @param accountId
     * @return status of indexing task for the given Account ID. This value is
     *         used to control execution of indexing tasks.
     */
    public int getTaskStatus(String accountId);

    /**
     * Set the status of indexing task for the given Account ID. This value is
     * used to control execution of indexing tasks.
     * 
     * @param accountId
     * @param status
     */
    public void setTaskStatus(String accountId, int status);
}
