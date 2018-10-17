import cv2
from matplotlib import pyplot as plt
import numpy as np

img = cv2.imread('taipei.jpg',0)
laplacian = cv2.Laplacian(img,cv2.CV_64F)


plt.imshow(img,cmap = 'gray')
plt.show()
plt.imshow(laplacian,cmap = 'gray')
plt.show()
def rgb2grey(rgb):
    if len(rgb.shape) is 3:
        return np.dot(rgb[...,:3], [0.299, 0.587, 0.114])
a = plt.imread('md.jpg')
a = rgb2grey(a)

print(a.shape)
plt.imshow(a, cmap='gray')
plt.show()
b = cv2.Laplacian(a, cv2.CV_64F)
b -= np.min(b)
b = b/np.max(b)
#b = b*255
#b = b.astype('int')

#b = b/np.max(b)
print(np.max(b), np.min(b))
plt.imshow(b, cmap='gray')
#plt.show()

plt.savefig('afterkb.jpg')
print(b.var())

#print(b)
"""print(np.mean(b), b.var())

b -= np.min(b)
b = b/np.max(b)

#b = b/np.max(b)
print(np.max(b), np.min(b))
plt.imshow(b, cmap='gray')
#plt.show()

b = b*255
b = b.astype('int')
plt.savefig('after.jpg')"""