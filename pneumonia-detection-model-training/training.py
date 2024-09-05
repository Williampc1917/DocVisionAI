import numpy as np
import random
import glob
import cv2
import tensorflow as tf
from tensorflow import keras
from keras.preprocessing.image import ImageDataGenerator
from keras.layers import Dense, Flatten, BatchNormalization, MaxPooling2D, Conv2D, Input
from keras.callbacks import EarlyStopping, ModelCheckpoint
from keras import Model

# Checking the availability of GPUs for training
print("Num GPUs Available: ", len(tf.config.list_physical_devices('GPU')))
# Splitting and organizing data. 80/20 split
train_normal = glob.glob('D:/pythonProject/Medical-GPU/X-ray/train/normal/*.jpeg')
train_pneumonia = glob.glob('D:/pythonProject/Medical-GPU/X-ray/train/pneumonia/*.jpeg')

test_normal = glob.glob('D:/pythonProject/Medical-GPU/X-ray/test/normal/*.jpeg')
test_pneumonia = glob.glob('D:/pythonProject/Medical-GPU/X-ray/test/pneumonia/*.jpeg')

val_normal = glob.glob('D:/pythonProject/Medical-GPU/X-ray/val/normal/*.jpeg')
val_pneumonia = glob.glob('D:/pythonProject/Medical-GPU/X-ray/val/pneumonia/*.jpeg')  # corrected the extension typo

# Merging data from train, test, and val directories
normal = train_normal + test_normal + val_normal
pneumonia = train_pneumonia + test_pneumonia + val_pneumonia

# Shuffling data
random.shuffle(normal)
random.shuffle(pneumonia)

# Splitting data into training and testing sets with 80/20 ratio
train_normal = normal[:int(len(normal) * 0.8)]
test_normal = normal[int(len(normal) * 0.8):]
train_pneumonia = pneumonia[:int(len(pneumonia) * 0.8)]
test_pneumonia = pneumonia[int(len(pneumonia) * 0.8):]

# Printing out the number of images in each split
print(f"Number of training normal images: {len(train_normal)}")
print(f"Number of testing normal images: {len(test_normal)}")
print(f"Number of training pneumonia images: {len(train_pneumonia)}")
print(f"Number of testing pneumonia images: {len(test_pneumonia)}")


# Function to load images from a given list of file paths
def load_images(file_paths, img_size=(256, 256)):
    # Loads and processes the images from the file paths
    images = []
    for path in file_paths:
        img = cv2.imread(path, cv2.IMREAD_COLOR)  # Reads the image in color
        img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)  # Converts from BGR to RGB (OpenCV loads images in BGR)
        img = cv2.resize(img, img_size)  # Resizes the image to the given size
        images.append(img)
    return np.array(images)


# Loading the training and testing images
train_normal_images = load_images(train_normal)
train_pneumonia_images = load_images(train_pneumonia)
test_normal_images = load_images(test_normal)
test_pneumonia_images = load_images(test_pneumonia)

# Creating labels for the images
y_train = np.concatenate([np.zeros(len(train_normal_images)), np.ones(len(train_pneumonia_images))])
y_test = np.concatenate([np.zeros(len(test_normal_images)), np.ones(len(test_pneumonia_images))])

# Merging the training and testing images into arrays
X_train = np.concatenate([train_normal_images, train_pneumonia_images], axis=0)
X_test = np.concatenate([test_normal_images, test_pneumonia_images], axis=0)


# Data augmentation and rescaling
train_datagen = ImageDataGenerator(rescale=1/255,
                                   rotation_range=40,
                                   width_shift_range=0.2,
                                   height_shift_range=0.2,
                                   shear_range=0.2,
                                   zoom_range=0.2,
                                   horizontal_flip=True,
                                   fill_mode='nearest')

test_datagen = ImageDataGenerator(rescale=1/255)

# Data generators
train_generator = train_datagen.flow(X_train, y_train, batch_size=16, shuffle=True)
test_generator = test_datagen.flow(X_test, y_test, batch_size=16, shuffle=False)

inception = keras.applications.inception_v3.InceptionV3(input_shape = (256,256,3),
                                                            include_top = False,
                                                            weights = 'imagenet')
# Loading the pre-trained InceptionV3 model without its top layer to use as a base for transfer learning
inception.trainable = False
last_output = inception.get_layer('mixed8').output

# Adding custom layers on top of the InceptionV3 base model
x = Flatten()(last_output)
x = Dense(64, activation='relu')(x)
x = BatchNormalization()(x)
x = Dense(32, activation='relu')(x)
x = BatchNormalization()(x)
x = Dense(1, activation='sigmoid')(x)

# Configuring the model for training
model = Model(inputs=inception.input, outputs=x)

# Setting up callbacks for early stopping and model checkpointing
callbacks = [
    EarlyStopping(monitor='val_loss', patience=7, restore_best_weights=True, verbose=1),
    ModelCheckpoint(filepath='pneumonia_xray_classifier.h5', monitor='val_loss', save_best_only=True)
]

model.compile(optimizer=keras.optimizers.Adam(learning_rate=1e-4),
              loss='binary_crossentropy',
              metrics=['accuracy'])
# Training the model with the train data, validation data, and callbacks defined earlier
history = model.fit(train_generator, epochs=50, validation_data=test_generator, callbacks=callbacks)

# After training, load the best model weights saved by the ModelCheckpoint callback
model.load_weights('pneumonia_xray_classifier.h5')