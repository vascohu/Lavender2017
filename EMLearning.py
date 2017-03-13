import numpy as np
import scipy.stats as st
import math as math

np.random.seed(1234)


# Generate the synthetic data : s\in {0,1,2}
def syn_label_gene(task_num, worker_num, label_num):
    __true_labels = st.bernoulli(0.5, loc=1).rvs(size=task_num)
    worker_quality = st.uniform(loc=0.5, scale=0.5).rvs(size=worker_num)
    sample_no = np.random.choice(task_num*worker_num, label_num, replace=False)
    __label_mat = np.zeros([task_num, worker_num])
    for i in sample_no:
        task_no = math.floor(i/worker_num)
        worker_no = i - task_no*worker_num
        true_or_false = st.bernoulli(worker_quality[worker_no]).rvs()
        if true_or_false:
            __label_mat[task_no][worker_no] = __true_labels[task_no]
        else:
            __label_mat[task_no][worker_no] = 3 - __true_labels[task_no]

    return __true_labels.astype(int), __label_mat.astype(int)


# Majority Voting to filter the true labels
def maj_voting(__label_mat, __label_num=2):
    __task_num = __label_mat.shape[0]
    __worker_num = __label_mat.shape[1]
    __label_dist = np.zeros([__task_num, __label_num], dtype=float)
    for i in range(__task_num):
        for j in range(__worker_num):
            __ob_label_ij = __label_mat[i, j]
            if __ob_label_ij > 0:
                __label_dist[i, __ob_label_ij-1] += 1
    __task_label_prob = np.zeros([__task_num, __label_num], dtype=float)
    for i in range(__task_num):
        __prob_sum = np.sum(__label_dist[i, :])
        if __prob_sum != 0:
            __task_label_prob[i, :] = __label_dist[i, :]/__prob_sum
        else:
            __task_label_prob[i, :].fill(1.0/__label_num)
    return __task_label_prob


# Online EM method to filter the true labels
def em_filter(__label_mat, __label_num=2):
    __task_num = __label_mat.shape[0]
    __worker_num = __label_mat.shape[1]

    # This tensor memorizes the probability for worker i to label the class j as k
    __label_tensor = np.zeros([__worker_num, __label_num, __label_num], dtype=float)

    # The labels is initialized as the results of majority voting
    __init_labels = maj_voting(__label_mat, __label_num)

    # Initialize the label tensor
    for i in range(__worker_num):
        __worker_labels = __label_mat[:, i]

        for j in range(__task_num):
            __real_label = __init_labels[j]
            __observed_label = __worker_labels[j]
            __label_tensor[i, __real_label-1, __observed_label-1] += 1

        for j in range(__label_num):
            __label_sum = np.sum(__label_tensor[i, j, :])
            if __label_sum == 0:
                __label_tensor[i, j, :] = np.ones(__label_num, dtype=float) * 1.0 / __label_num
            else:
                __label_tensor[i, j, :] /= __label_sum

    # E-M iteration to filter the true labels
    __label_tensor0 = np.zeros([__worker_num, __label_num, __label_num], dtype=float)
    __max_diff = np.amax(np.absolute(__label_tensor-__label_tensor0))
    __label_dist = np.zeros([__task_num, __label_num], dtype=float)
    while __max_diff > 1e-6:
        '''E step: find the estimation of label distribution'''
        __label_dist.fill(0.0)
        for i in range(__task_num):
            for j in range(__worker_num):
                __ob_label_ij = __label_mat[i, j]
                __prob_inc_vec = np.log(__label_tensor[j, :, __ob_label_ij-1])
                __label_dist[i, :] += __prob_inc_vec
        __task_label_prob = np.exp(__label_dist)
        for i in range(__task_num):
            __task_label_prob[i, :] /= np.sum(__task_label_prob[i, :])
        '''M step: update the estimate of the tensor'''
        __label_tensor0 = __label_tensor
        __label_tensor.fill(0)
        for i in range(__task_num):
            for j in range(__worker_num):
                __ob_label_ij = __label_mat[i, j]
                __label_tensor[j, :, __ob_label_ij-1] += __task_label_prob[i, :]
        for i in range(__worker_num):
            for j in range(__label_num):
                __prob_sum = np.sum(__label_tensor[i, j, :])
                if __prob_sum != 0:
                    __label_tensor[i, j, :] /= __prob_sum
                else:
                    __label_tensor[i, j, :].fill(1.0/__label_num)
        '''Calculate the difference'''
        __max_diff = np.amax(np.absolute(__label_tensor - __label_tensor0))

    # Re-estimate the probabilities of labels
    __label_dist.fill(0.0)
    for i in range(__task_num):
        for j in range(__worker_num):
            __ob_label_ij = __label_mat[i, j]
            __prob_inc_vec = np.log(__label_tensor[j, :, __ob_label_ij - 1])
            __label_dist[i, :] += __prob_inc_vec
    __task_label_prob = np.exp(__label_dist)
    for i in range(__task_num):
        __task_label_prob[i, :] /= np.sum(__task_label_prob[i, :])
    return __label_tensor, __task_label_prob


if __name__ == "__main__":
    (true_labels, label_mat) = syn_label_gene(10, 10, 10)
    print(true_labels)
    print(label_mat)
    task_label_prob = maj_voting(label_mat)
    #label_tensor = em_filter(label_mat)
    print(task_label_prob)
    #print(label_tensor)
