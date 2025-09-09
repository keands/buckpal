import React, { useState, useEffect } from 'react';
import { X, Calendar, DollarSign, Tag, Clock } from 'lucide-react';

interface RecurringPaymentFormData {
  name: string;
  description: string;
  amount: number;
  paymentType: 'INCOME' | 'EXPENSE' | 'CREDIT' | 'SUBSCRIPTION';
  frequency: 'WEEKLY' | 'MONTHLY' | 'BIMONTHLY' | 'QUARTERLY' | 'BIANNUAL' | 'ANNUAL';
  startDate: string;
  endDate?: string;
  remainingPayments?: number;
  escalationRate: number;
  color?: string;
  icon?: string;
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

interface RecurringPaymentFormProps {
  onSubmit: (formData: RecurringPaymentFormData) => Promise<void>;
  onCancel: () => void;
  initialData?: Partial<RecurringPaymentFormData>;
  paymentTypes: Record<string, PaymentType>;
  frequencies: Record<string, PaymentFrequency>;
  loading?: boolean;
}

const RecurringPaymentForm: React.FC<RecurringPaymentFormProps> = ({
  onSubmit,
  onCancel,
  initialData,
  paymentTypes,
  frequencies,
  loading = false
}) => {
  const [formData, setFormData] = useState<RecurringPaymentFormData>({
    name: '',
    description: '',
    amount: 0,
    paymentType: 'EXPENSE',
    frequency: 'MONTHLY',
    startDate: new Date().toISOString().split('T')[0],
    escalationRate: 0,
    ...initialData
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [endDateType, setEndDateType] = useState<'none' | 'date' | 'payments'>('none');

  useEffect(() => {
    // Set default color and icon based on payment type
    if (paymentTypes[formData.paymentType] && !formData.color) {
      setFormData(prev => ({
        ...prev,
        color: paymentTypes[formData.paymentType].defaultColor,
        icon: paymentTypes[formData.paymentType].defaultIcon
      }));
    }
  }, [formData.paymentType, paymentTypes]);

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.name.trim()) {
      newErrors.name = 'Le nom est requis';
    }

    if (formData.amount <= 0) {
      newErrors.amount = 'Le montant doit être positif';
    }

    if (!formData.startDate) {
      newErrors.startDate = 'La date de début est requise';
    }

    if (endDateType === 'date' && formData.endDate && new Date(formData.endDate) <= new Date(formData.startDate)) {
      newErrors.endDate = 'La date de fin doit être après la date de début';
    }

    if (endDateType === 'payments' && (!formData.remainingPayments || formData.remainingPayments <= 0)) {
      newErrors.remainingPayments = 'Le nombre de paiements doit être positif';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    // Prepare final form data
    const finalData = { ...formData };
    
    if (endDateType !== 'date') {
      delete finalData.endDate;
    }
    
    if (endDateType !== 'payments') {
      delete finalData.remainingPayments;
    }

    try {
      await onSubmit(finalData);
    } catch (error) {
      console.error('Error submitting form:', error);
    }
  };

  const handleInputChange = (field: keyof RecurringPaymentFormData, value: any) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
    
    // Clear error when user starts typing
    if (errors[field]) {
      setErrors(prev => ({
        ...prev,
        [field]: ''
      }));
    }
  };

  return (
    <div 
      className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50"
      onClick={onCancel}
    >
      <div 
        className="bg-white rounded-lg p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex justify-between items-center mb-6">
          <h3 className="text-xl font-semibold text-gray-900">
            {initialData ? 'Modifier le paiement récurrent' : 'Ajouter un paiement récurrent'}
          </h3>
          <button
            onClick={onCancel}
            className="text-gray-400 hover:text-gray-600"
          >
            <X className="h-6 w-6" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Basic Information */}
          <div className="space-y-4">
            <h4 className="text-lg font-medium text-gray-900 flex items-center">
              <Tag className="h-5 w-5 mr-2" />
              Informations générales
            </h4>

            <div>
              <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
                Nom du paiement *
              </label>
              <input
                id="name"
                type="text"
                value={formData.name}
                onChange={(e) => handleInputChange('name', e.target.value)}
                placeholder="Ex: Salaire, Loyer, Crédit auto..."
                className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  errors.name ? 'border-red-500' : 'border-gray-300'
                }`}
              />
              {errors.name && <p className="text-red-500 text-sm mt-1">{errors.name}</p>}
            </div>

            <div>
              <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
                Description
              </label>
              <textarea
                id="description"
                value={formData.description}
                onChange={(e) => handleInputChange('description', e.target.value)}
                placeholder="Description optionnelle..."
                rows={3}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          {/* Amount and Type */}
          <div className="space-y-4">
            <h4 className="text-lg font-medium text-gray-900 flex items-center">
              <DollarSign className="h-5 w-5 mr-2" />
              Montant et type
            </h4>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label htmlFor="amount" className="block text-sm font-medium text-gray-700 mb-1">
                  Montant *
                </label>
                <input
                  id="amount"
                  type="number"
                  step="0.01"
                  min="0"
                  value={formData.amount}
                  onChange={(e) => handleInputChange('amount', parseFloat(e.target.value) || 0)}
                  className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                    errors.amount ? 'border-red-500' : 'border-gray-300'
                  }`}
                />
                {errors.amount && <p className="text-red-500 text-sm mt-1">{errors.amount}</p>}
              </div>

              <div>
                <label htmlFor="paymentType" className="block text-sm font-medium text-gray-700 mb-1">
                  Type de paiement *
                </label>
                <select
                  id="paymentType"
                  value={formData.paymentType}
                  onChange={(e) => handleInputChange('paymentType', e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  {Object.entries(paymentTypes).map(([key, type]) => (
                    <option key={key} value={key}>
                      {type.displayName}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div>
              <label htmlFor="escalationRate" className="block text-sm font-medium text-gray-700 mb-1">
                Taux d'escalade annuel (%)
              </label>
              <input
                id="escalationRate"
                type="number"
                step="0.1"
                min="0"
                max="100"
                value={formData.escalationRate}
                onChange={(e) => handleInputChange('escalationRate', parseFloat(e.target.value) || 0)}
                placeholder="0"
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <p className="text-sm text-gray-500 mt-1">
                Augmentation annuelle automatique (ex: 2.5% pour l'inflation)
              </p>
            </div>
          </div>

          {/* Schedule */}
          <div className="space-y-4">
            <h4 className="text-lg font-medium text-gray-900 flex items-center">
              <Clock className="h-5 w-5 mr-2" />
              Planification
            </h4>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label htmlFor="frequency" className="block text-sm font-medium text-gray-700 mb-1">
                  Fréquence *
                </label>
                <select
                  id="frequency"
                  value={formData.frequency}
                  onChange={(e) => handleInputChange('frequency', e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  {Object.entries(frequencies).map(([key, freq]) => (
                    <option key={key} value={key}>
                      {freq.displayName}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label htmlFor="startDate" className="block text-sm font-medium text-gray-700 mb-1">
                  Date de début *
                </label>
                <input
                  id="startDate"
                  type="date"
                  value={formData.startDate}
                  onChange={(e) => handleInputChange('startDate', e.target.value)}
                  className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                    errors.startDate ? 'border-red-500' : 'border-gray-300'
                  }`}
                />
                {errors.startDate && <p className="text-red-500 text-sm mt-1">{errors.startDate}</p>}
              </div>
            </div>

            {/* End Date Options */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Fin du paiement
              </label>
              <div className="space-y-3">
                <label className="flex items-center">
                  <input
                    type="radio"
                    name="endDateType"
                    checked={endDateType === 'none'}
                    onChange={() => setEndDateType('none')}
                    className="mr-2"
                  />
                  Indéfini (jusqu'à annulation manuelle)
                </label>
                
                <label className="flex items-center">
                  <input
                    type="radio"
                    name="endDateType"
                    checked={endDateType === 'date'}
                    onChange={() => setEndDateType('date')}
                    className="mr-2"
                  />
                  Date de fin spécifique
                </label>
                
                {endDateType === 'date' && (
                  <div className="ml-6">
                    <input
                      type="date"
                      value={formData.endDate || ''}
                      onChange={(e) => handleInputChange('endDate', e.target.value)}
                      className={`px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                        errors.endDate ? 'border-red-500' : 'border-gray-300'
                      }`}
                    />
                    {errors.endDate && <p className="text-red-500 text-sm mt-1">{errors.endDate}</p>}
                  </div>
                )}
                
                <label className="flex items-center">
                  <input
                    type="radio"
                    name="endDateType"
                    checked={endDateType === 'payments'}
                    onChange={() => setEndDateType('payments')}
                    className="mr-2"
                  />
                  Nombre de paiements limité
                </label>
                
                {endDateType === 'payments' && (
                  <div className="ml-6">
                    <input
                      type="number"
                      min="1"
                      value={formData.remainingPayments || ''}
                      onChange={(e) => handleInputChange('remainingPayments', parseInt(e.target.value) || undefined)}
                      placeholder="Nombre de paiements"
                      className={`px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                        errors.remainingPayments ? 'border-red-500' : 'border-gray-300'
                      }`}
                    />
                    {errors.remainingPayments && <p className="text-red-500 text-sm mt-1">{errors.remainingPayments}</p>}
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Form Actions */}
          <div className="flex justify-end space-x-3 pt-6 border-t">
            <button
              type="button"
              onClick={onCancel}
              className="px-4 py-2 text-gray-700 bg-gray-100 rounded-md hover:bg-gray-200"
            >
              Annuler
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
            >
              {loading ? 'Enregistrement...' : (initialData ? 'Modifier' : 'Créer')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default RecurringPaymentForm;