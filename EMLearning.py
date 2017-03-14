import numpy as np
import scipy.stats as st
import math as math

np.random.seed(1230)


# Generate the synthetic data : s\in {0,1,2}
def syn_label_gene(task_num, worker_num, label_num):
    __true_labels = st.bernoulli(0.5, loc=1).rvs(size=task_num)
    worker_quality = st.uniform(loc=0.4, scale=0.5).rvs(size=worker_num)
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

    return __true_labels.astype(int), __label_mat.astype(int), worker_quality


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
    __init_task_label_prob = maj_voting(__label_mat, __label_num)

    # Initialize the label tensor
    for i in range(__task_num):
        for j in range(__worker_num):
            __ob_label_ij = __label_mat[i, j]
            if __ob_label_ij > 0:
                __label_tensor[j, :, __ob_label_ij - 1] += __init_task_label_prob[i, :]
    for i in range(__worker_num):
        for j in range(__label_num):
            __prob_sum = np.sum(__label_tensor[i, j, :])
            if __prob_sum != 0:
                __label_tensor[i, j, :] /= __prob_sum
            else:
                __label_tensor[i, j, :].fill(1.0 / __label_num)

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
                if __ob_label_ij > 0:
                    __prob_inc_vec = np.log(__label_tensor[j, :, __ob_label_ij-1]+1e-20)
                    __label_dist[i, :] += __prob_inc_vec
        __task_label_prob = np.exp(__label_dist)
        for i in range(__task_num):
            __task_label_prob[i, :] /= np.sum(__task_label_prob[i, :])
        '''M step: update the estimate of the tensor'''
        __label_tensor0 = __label_tensor.copy()
        __label_tensor.fill(0)
        for i in range(__task_num):
            for j in range(__worker_num):
                __ob_label_ij = __label_mat[i, j]
                if __ob_label_ij > 0:
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
            __prob_inc_vec = np.log(__label_tensor[j, :, __ob_label_ij - 1]+1e-20)
            __label_dist[i, :] += __prob_inc_vec
    __task_label_prob = np.exp(__label_dist)
    for i in range(__task_num):
        __task_label_prob[i, :] /= np.sum(__task_label_prob[i, :])
    return __label_tensor, __task_label_prob


def prob_to_label(task_label_prob):
    __task_num = task_label_prob.shape[0]
    __labels = np.zeros(__task_num)
    for i in range(__task_num):
        __labels[i] = np.argmax(task_label_prob[i, :]) + 1
    return __labels


def label_accuracy(__label1, __label2):
    __task_num = __label1.shape[0]
    __same_label_num = 0
    for i in range(__task_num):
        if __label1[i] == __label2[i]:
            __same_label_num += 1
    return __same_label_num/__task_num

if __name__ == "__main__":
    worker_num = 10
    task_num = 100
    acc_data = []
    fp = open('result.txt', 'w')
    for i in range(1):
        (true_labels, label_mat, worker_qua) = syn_label_gene(task_num, worker_num, task_num * worker_num)
        task_label_prob_mj = maj_voting(label_mat)
        label_mj = prob_to_label(task_label_prob_mj)
        accuracy_mj = label_accuracy(true_labels, label_mj)
        print("MJ Accuracy: ", accuracy_mj)
        (label_tensor, task_label_prob_em) = em_filter(label_mat)
        label_em = prob_to_label(task_label_prob_em)
        accuracy_em = label_accuracy(true_labels, label_em)
        print("EM Accuracy: ", accuracy_em)
        acc_data.append([accuracy_mj, accuracy_em])
        # formulated = "%.5f" % accuracy_em
        fp.write("{:.5f}".format(accuracy_mj)+'\t'+"{:.5f}".format(accuracy_em)+'\n')
        for j in range(10):
            print(true_labels[j], '\t', task_label_prob_mj[j, :], '\t', task_label_prob_em[j, :])
        for j in range(10):
            print(worker_qua[j], '\n', label_tensor[j, :, :])

    # print(acc_data)
    # fp.close()
    # print(online_labels)
    # print(task_label_prob)
    # print(task_label_prob_em)



