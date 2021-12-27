package http;

import java.nio.charset.Charset;
import java.util.NoSuchElementException;

public class ByteBuffer {

	private final Node firstNode;
	private Node currentNode;
	private final int singleNodeCapacity;
	private int numberOfNodes;
	

	public ByteBuffer(int initialCapacity) {
		if (initialCapacity < 1) {
			throw new IllegalArgumentException("Initial capacity must be at least 1.");
		}
		this.firstNode = new Node(initialCapacity, null);
		this.currentNode = firstNode;
		this.singleNodeCapacity = initialCapacity;
		this.numberOfNodes = 1;
	}
	
	public ByteBuffer() {
		this(512);
	}
	
	public void put(byte b) {
		boolean succeded = currentNode.put(b);
		if (!succeded) { // capacity of current node is full
			currentNode = currentNode.createNext(singleNodeCapacity);
			numberOfNodes++;
			currentNode.put(b);
		}
	}
	
	public boolean empty() {
		return currentNode == firstNode && currentNode.position == 0;
	}
	
	public byte peek() {
		if (currentNode.position > 0) {
			return currentNode.buffer[currentNode.position - 1];
		} else if (currentNode == firstNode) {
			throw new NoSuchElementException();
		} else {
			byte[] previousBuffer = currentNode.previousNode.buffer;
			return previousBuffer[previousBuffer.length - 1];
		}
	}
	
	public byte pop() {
		if (currentNode.position > 0) {
			currentNode.position--;
			return currentNode.buffer[currentNode.position];
		} else if (currentNode == firstNode) {
			throw new NoSuchElementException();
		} else {
			currentNode = currentNode.previousNode;
			numberOfNodes--;
			currentNode.hasNext = false;
			currentNode.position = currentNode.buffer.length;
			return currentNode.buffer[currentNode.position - 1];
		}
	}
	
	public int length() {
		return (numberOfNodes - 1) * singleNodeCapacity + currentNode.position;
	}
	
	public String read(Charset charset) {
		if (numberOfNodes == 1) {
			return new String(firstNode.buffer, 0, firstNode.position, charset);
		} else {
			byte[] concat = new byte[this.length()];
			int written = 0;
			Node node = firstNode;
			while (true) {
				for (int j = 0; j < node.position; j++) {
					concat[written] = node.buffer[j];
					written++;
				}
				if (node.hasNext) {
					node = node.nextNode;
				} else {
					break;
				}
			}
			return new String(concat, charset);
		}
	}
	
	public void reset() {
		Node node = firstNode;
		while (true) {
			node.reset();
			if (node.hasNext) {
				node = node.nextNode;
			} else {
				break;
			}
		}
		currentNode = firstNode;
		numberOfNodes = 1;
	}
	
	private static class Node {
		
		private final byte[] buffer;
		private int position; // index of current position for writing in buffer
		private Node nextNode;
		private Node previousNode;
		private boolean hasNext;
		
		private Node(int capacity, Node previous) {
			this.buffer = new byte[capacity];
			this.position = 0;
			this.nextNode = null;
			this.previousNode = previous;
			this.hasNext = false;
		}
		
		private Node createNext(int capacity) { // if it doesn't exist already; otherwise reuse a chain
			if (nextNode == null) {
				nextNode = new Node(capacity, this);
			}
			hasNext = true;
			return this.nextNode;
		}
		
		private boolean put(byte b) {
			if (position < buffer.length) {
				buffer[position] = b;
				position++;
				return true;
			} else { // position == buffer.length
				return false;
			}
		}
		
		private void reset() {
			position = 0;
			hasNext = false;
		}
		
	}
	
}
