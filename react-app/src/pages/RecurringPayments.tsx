import React, { useState, useEffect } from 'react';
import { Plus, Calendar, DollarSign, TrendingUp, TrendingDown, CreditCard, RefreshCw, Edit2, Trash2 } from 'lucide-react';
import { format } from 'date-fns';
import { fr } from 'date-fns/locale';
import RecurringPaymentForm from '@/components/RecurringPaymentForm';
import BudgetProjection from '@/components/BudgetProjection';
import { apiClient } from '@/lib/api';

interface RecurringPayment {
  id: number;
  name: string;
  description: string;
  amount: number;
  paymentType: 'INCOME' | 'EXPENSE' | 'CREDIT' | 'SUBSCRIPTION';
  frequency: 'WEEKLY' | 'MONTHLY' | 'BIMONTHLY' | 'QUARTERLY' | 'BIANNUAL' | 'ANNUAL';
  startDate: string;
  endDate?: string;
  remainingPayments?: number;
  escalationRate: number;
  color: string;
  icon: string;
  isActive: boolean;
  createdAt: string;
}

interface PaymentType {
  name: string;
  displayName: string;
  description: string;
  defaultColor: string;
  defaultIcon: string;
}

interface PaymentFrequency {
  name: string;
  displayName: string;
  description: string;
  monthsInterval: number;
  isWeekly: boolean;
}

const RecurringPayments: React.FC = () => {
  const [payments, setPayments] = useState<RecurringPayment[]>([]);
  const [paymentTypes, setPaymentTypes] = useState<Record<string, PaymentType>>({});
  const [frequencies, setFrequencies] = useState<Record<string, PaymentFrequency>>({});
  const [statistics, setStatistics] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [editingPayment, setEditingPayment] = useState<RecurringPayment | null>(null);
  const [formLoading, setFormLoading] = useState(false);
  const [showProjection, setShowProjection] = useState(false);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      
      // Fetch all data in parallel using authenticated API client
      const [paymentsData, typesData, freqData, statsData] = await Promise.all([
        apiClient.getRecurringPayments(),
        apiClient.getRecurringPaymentTypes(),
        apiClient.getRecurringPaymentFrequencies(),
        apiClient.getRecurringPaymentStatistics()
      ]);

      // Validate data structure
      if (!Array.isArray(paymentsData)) {
        throw new Error('Payments data is not an array');
      }

      setPayments(paymentsData);
      setPaymentTypes(typesData);
      setFrequencies(freqData);
      setStatistics(statsData);
    } catch (error) {
      console.error('Error fetching recurring payments data:', error);
      throw error; // Re-throw to let the error surface properly
    } finally {
      setLoading(false);
    }
  };

  const getPaymentTypeIcon = (type: string) => {
    switch (type) {
      case 'INCOME':
        return <TrendingUp className="h-5 w-5 text-green-600" />;
      case 'EXPENSE':
        return <TrendingDown className="h-5 w-5 text-red-600" />;
      case 'CREDIT':
        return <CreditCard className="h-5 w-5 text-orange-600" />;
      case 'SUBSCRIPTION':
        return <RefreshCw className="h-5 w-5 text-blue-600" />;
      default:
        return <DollarSign className="h-5 w-5 text-gray-600" />;
    }
  };

  const getPaymentTypeColor = (type: string) => {
    switch (type) {
      case 'INCOME':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'EXPENSE':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'CREDIT':
        return 'bg-orange-100 text-orange-800 border-orange-200';
      case 'SUBSCRIPTION':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR'
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return format(new Date(dateString), 'dd MMM yyyy', { locale: fr });
  };

  const handleDeletePayment = async (id: number) => {
    if (!confirm('Êtes-vous sûr de vouloir supprimer ce paiement récurrent ?')) {
      return;
    }

    try {
      await apiClient.deleteRecurringPayment(id);
      await fetchData(); // Refresh data
    } catch (error) {
      console.error('Error deleting payment:', error);
    }
  };

  const handleCreatePayment = async (formData: any) => {
    try {
      setFormLoading(true);
      
      await apiClient.createRecurringPayment(formData);
      await fetchData(); // Refresh data
      setShowCreateForm(false);
    } catch (error) {
      console.error('Error creating payment:', error);
      throw error;
    } finally {
      setFormLoading(false);
    }
  };

  const handleEditPayment = async (formData: any) => {
    if (!editingPayment) return;
    
    try {
      setFormLoading(true);
      
      await apiClient.updateRecurringPayment(editingPayment.id, formData);
      await fetchData(); // Refresh data
      setEditingPayment(null);
    } catch (error) {
      console.error('Error updating payment:', error);
      throw error;
    } finally {
      setFormLoading(false);
    }
  };

  const handleCloseForm = () => {
    setShowCreateForm(false);
    setEditingPayment(null);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto p-6 space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Paiements récurrents</h1>
          <p className="text-gray-600">Gérez vos revenus et dépenses récurrents</p>
        </div>
        <div className="flex gap-3">
          <button
            onClick={() => setShowProjection(true)}
            className="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 flex items-center gap-2"
          >
            <Calendar className="h-4 w-4" />
            Voir les projections
          </button>
          <button
            onClick={() => setShowCreateForm(true)}
            className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 flex items-center gap-2"
          >
            <Plus className="h-4 w-4" />
            Ajouter un paiement
          </button>
        </div>
      </div>

      {/* Statistics Cards */}
      {statistics && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          <div className="bg-white p-6 rounded-lg shadow-sm border">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Paiements actifs</p>
                <p className="text-2xl font-semibold text-gray-900">
                  {statistics.totalActivePayments || 0}
                </p>
              </div>
              <RefreshCw className="h-8 w-8 text-blue-600" />
            </div>
          </div>

          <div className="bg-white p-6 rounded-lg shadow-sm border">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Revenus mensuels</p>
                <p className="text-2xl font-semibold text-green-600">
                  {statistics.monthlyAmountsByType?.INCOME 
                    ? formatCurrency(statistics.monthlyAmountsByType.INCOME)
                    : formatCurrency(0)}
                </p>
              </div>
              <TrendingUp className="h-8 w-8 text-green-600" />
            </div>
          </div>

          <div className="bg-white p-6 rounded-lg shadow-sm border">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Dépenses mensuelles</p>
                <p className="text-2xl font-semibold text-red-600">
                  {statistics.monthlyAmountsByType?.EXPENSE 
                    ? formatCurrency(statistics.monthlyAmountsByType.EXPENSE)
                    : formatCurrency(0)}
                </p>
              </div>
              <TrendingDown className="h-8 w-8 text-red-600" />
            </div>
          </div>

          <div className="bg-white p-6 rounded-lg shadow-sm border">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Crédits mensuels</p>
                <p className="text-2xl font-semibold text-orange-600">
                  {statistics.monthlyAmountsByType?.CREDIT 
                    ? formatCurrency(statistics.monthlyAmountsByType.CREDIT)
                    : formatCurrency(0)}
                </p>
              </div>
              <CreditCard className="h-8 w-8 text-orange-600" />
            </div>
          </div>
        </div>
      )}

      {/* Payments List */}
      <div className="bg-white rounded-lg shadow-sm border">
        <div className="px-6 py-4 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">Vos paiements récurrents</h2>
        </div>
        
        <div className="divide-y divide-gray-200">
          {!Array.isArray(payments) || payments.length === 0 ? (
            <div className="p-8 text-center">
              <Calendar className="h-12 w-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">Aucun paiement récurrent</h3>
              <p className="text-gray-600 mb-4">
                Commencez par ajouter vos revenus et dépenses récurrents pour une meilleure gestion budgétaire.
              </p>
              <button
                onClick={() => setShowCreateForm(true)}
                className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700"
              >
                Ajouter votre premier paiement
              </button>
            </div>
          ) : (
            payments.map((payment) => (
              <div key={payment.id} className="p-6 hover:bg-gray-50">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-4">
                    <div className="flex-shrink-0">
                      {getPaymentTypeIcon(payment.paymentType)}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center space-x-2">
                        <p className="text-sm font-medium text-gray-900 truncate">
                          {payment.name}
                        </p>
                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${getPaymentTypeColor(payment.paymentType)}`}>
                          {paymentTypes[payment.paymentType]?.displayName || payment.paymentType}
                        </span>
                      </div>
                      <p className="text-sm text-gray-600 truncate">
                        {payment.description}
                      </p>
                      <div className="flex items-center space-x-4 mt-1 text-xs text-gray-500">
                        <span>
                          {frequencies[payment.frequency]?.displayName || payment.frequency}
                        </span>
                        <span>
                          Depuis le {formatDate(payment.startDate)}
                        </span>
                        {payment.endDate && (
                          <span>
                            Jusqu'au {formatDate(payment.endDate)}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                  
                  <div className="flex items-center space-x-4">
                    <div className="text-right">
                      <p className={`text-sm font-semibold ${
                        payment.paymentType === 'INCOME' ? 'text-green-600' : 'text-gray-900'
                      }`}>
                        {payment.paymentType === 'INCOME' ? '+' : '-'}{formatCurrency(Math.abs(payment.amount))}
                      </p>
                      <p className="text-xs text-gray-500">
                        {frequencies[payment.frequency]?.displayName || payment.frequency}
                      </p>
                    </div>
                    
                    <div className="flex items-center space-x-2">
                      <button 
                        onClick={() => setEditingPayment(payment)}
                        className="p-2 text-gray-400 hover:text-blue-600"
                      >
                        <Edit2 className="h-4 w-4" />
                      </button>
                      <button 
                        onClick={() => handleDeletePayment(payment.id)}
                        className="p-2 text-gray-400 hover:text-red-600"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Create/Edit Form */}
      {(showCreateForm || editingPayment) && (
        <RecurringPaymentForm
          onSubmit={editingPayment ? handleEditPayment : handleCreatePayment}
          onCancel={handleCloseForm}
          initialData={editingPayment ? {
            name: editingPayment.name,
            description: editingPayment.description,
            amount: editingPayment.amount,
            paymentType: editingPayment.paymentType,
            frequency: editingPayment.frequency,
            startDate: editingPayment.startDate,
            endDate: editingPayment.endDate,
            remainingPayments: editingPayment.remainingPayments,
            escalationRate: editingPayment.escalationRate,
            color: editingPayment.color,
            icon: editingPayment.icon
          } : undefined}
          paymentTypes={paymentTypes}
          frequencies={frequencies}
          loading={formLoading}
        />
      )}

      {/* Budget Projection Modal */}
      {showProjection && (
        <div 
          className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50"
          onClick={() => setShowProjection(false)}
        >
          <div 
            className="w-full max-w-6xl max-h-[90vh] overflow-auto"
            onClick={(e) => e.stopPropagation()}
          >
            <BudgetProjection onClose={() => setShowProjection(false)} />
          </div>
        </div>
      )}
    </div>
  );
};

export default RecurringPayments;