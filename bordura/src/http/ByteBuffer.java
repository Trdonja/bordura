package http;

import java.nio.charset.Charset;

public class ByteBuffer {

	private final Node firstNode;
	private Node currentNode;
	private final int singleNodeCapacity;
	private int numberOfNodes;
	

	public ByteBuffer(int initialCapacity) {
		if (initialCapacity < 1) {
			throw new IllegalArgumentException("Initial capacity must be at least 1.");
		}
		this.firstNode = new Node(initialCapacity);
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
		private boolean hasNext;
		
		private Node(int capacity) {
			this.buffer = new byte[capacity];
			this.position = 0;
			this.nextNode = null;
			this.hasNext = false;
		}
		
		private Node createNext(int capacity) { // if it doesn't exist already; otherwise reuse a chain
			if (nextNode == null) {
				nextNode = new Node(capacity);
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
